package com.stone.wms.application.service;

import com.stone.wms.application.dto.StockWorkDetailDto;
import com.stone.wms.domain.exception.DispatchException;
import com.stone.wms.domain.model.dispatch.DispatchTask;
import com.stone.wms.domain.model.dispatch.DispatchTask.State;
import com.stone.wms.domain.model.inventory.*;
import com.stone.wms.domain.repository.InventoryRepository;
import com.stone.wms.domain.repository.StockWorkRepository;
import com.stone.wms.domain.repository.StockWorkRepository.Location;
import com.stone.wms.domain.repository.StockWorkRepository.Receipt;
import org.springframework.stereotype.Service;

import java.util.Objects;

import static com.stone.wms.domain.exception.DispatchException.Code.CONFLICT;
import static com.stone.wms.domain.exception.DispatchException.Code.NOT_FOUND;

@Service
class StockWorkService {
    private final StockWorkRepository repository;
    private final InventoryRepository inventory;

    StockWorkService(StockWorkRepository repository, InventoryRepository inventory) {
        this.repository = repository;
        this.inventory = inventory;
    }

    StockWorkDetailDto detail(long warehouse, DispatchTask task) {
        return new StockWorkDetailDto(task, repository.ledger(warehouse, task.id));
    }

    void registerReceipt(long warehouse, String key, long sku, String lot, int quantity) {
        require(sku > 0 && quantity > 0, "实收商品和数量必须为正数");
        Receipt existing = repository.receipt(warehouse, key);
        if (existing != null) {
            require(existing.skuId == sku && existing.lotNo.equals(lot) && existing.receivedQty == quantity,
                    "相同收货明细键的内容不一致");
            return;
        }
        repository.insertReceipt(warehouse, key, sku, lot, quantity);
    }

    void reserve(long warehouse, DispatchTask task, StockWork work) {
        require(work.skuId > 0 && work.quantity > 0 && work.destinationId > 0, "商品、库位和数量必须为正数");
        require(validWork(work), "上架须引用收货明细；移库须指定与目标不同的源库位");
        Location destination = lockEndpoints(warehouse, work);
        require(destination.mixPolicy != null && destination.maxUnits > 0, "库位容量或混放策略未正确配置");
        long occupied = repository.onHand(warehouse, work.destinationId) + repository.incoming(warehouse, work.destinationId);
        require(occupied + work.quantity <= destination.maxUnits, "目标库位可用容量不足");
        require(!repository.hasMixConflict(warehouse, work, destination.mixPolicy), "目标库位违反商品或批次混放策略");

        if (work.type == StockWork.Type.PUTAWAY) {
            Receipt receipt = repository.receipt(warehouse, work.receiptKey);
            if (receipt == null) {
                throw new DispatchException(NOT_FOUND, "收货明细不存在");
            }
            require(receipt.skuId == work.skuId && receipt.lotNo.equals(work.lotNo), "任务商品或批次与收货明细不一致");
            require(repository.committedReceipt(warehouse, work.receiptKey) + work.quantity <= receipt.receivedQty,
                    "已完成和待执行的上架量超出实收量");
        } else {
            changeSource(warehouse, task, work, "MOVE_RESERVE", source -> {
                require(source.getAvailableQty() >= work.quantity, "源库位可用库存不足");
                source.reserve(work.quantity);
            });
        }
        repository.insertWork(task.id, warehouse, work);
    }

    private boolean validWork(StockWork work) {
        if (work.type == StockWork.Type.PUTAWAY) {
            return work.receiptKey != null && work.sourceId == null;
        }
        return work.type == StockWork.Type.TRANSFER && work.receiptKey == null && work.sourceId != null
                && work.sourceId > 0 && work.sourceId.longValue() != work.destinationId;
    }

    void finish(long warehouse, DispatchTask task, State next, Integer completedQty) {
        StockWork work = task.work;
        if (work == null) {
            require(completedQty == null, "通用任务不使用库存作业数量字段");
            return;
        }
        if (next == State.UNKNOWN) {
            require(completedQty == null || (completedQty >= 0 && completedQty <= work.quantity), "实作数量超出任务数量");
            return;
        }
        if (next != State.CANCELLED) {
            int expected = next == State.SUCCEEDED ? work.quantity : 0;
            require(Objects.equals(completedQty, expected),
                    "成功须报全量，失败须确认零作业；部分完成或结果不明请报告 UNKNOWN");
        }

        lockEndpoints(warehouse, work);
        if (work.type == StockWork.Type.TRANSFER) {
            if (next == State.SUCCEEDED) {
                changeSource(warehouse, task, work, "MOVE_OUT", source -> source.ship(work.quantity));
            } else {
                changeSource(warehouse, task, work, "MOVE_RELEASE", source -> source.release(work.quantity));
            }
        }
        if (next == State.SUCCEEDED) {
            putAway(warehouse, task, work);
        }
    }

    boolean freeze(long warehouse, long locationId, String key, String reason) {
        Location location = location(warehouse, locationId);
        String previous = repository.freezeReason(warehouse, locationId, key);
        if (previous != null) {
            require(previous.equals(reason), "相同冻结键的原因不一致");
            return key.equals(location.freezeKey);
        }
        require(location.freezeKey == null, "已有其他作业冻结该库位");
        require(!repository.hasActiveStockOrWork(warehouse, locationId), "库位仍有预占或未确认的库存作业");
        repository.openFreeze(warehouse, locationId, key, reason);
        return true;
    }

    boolean unfreeze(long warehouse, long locationId, String key) {
        Location location = location(warehouse, locationId);
        if (repository.freezeReason(warehouse, locationId, key) == null) {
            throw new DispatchException(NOT_FOUND, "冻结记录不存在");
        }
        if (key.equals(location.freezeKey)) {
            repository.closeFreeze(warehouse, locationId, key);
        }
        return false;
    }

    private Location lockEndpoints(long warehouse, StockWork work) {
        boolean sourceFirst = work.sourceId != null && work.sourceId < work.destinationId;
        if (sourceFirst) {
            requireOpen(warehouse, work.sourceId);
        }
        Location destination = requireOpen(warehouse, work.destinationId);
        if (work.sourceId != null && !sourceFirst) {
            requireOpen(warehouse, work.sourceId);
        }
        return destination;
    }

    private Location requireOpen(long warehouse, long id) {
        Location location = location(warehouse, id);
        require(location.freezeKey == null, "库位已冻结");
        return location;
    }

    private Location location(long warehouse, long id) {
        Location location = repository.lockLocation(warehouse, id);
        if (location == null) {
            throw new DispatchException(NOT_FOUND, "库位尚未配置库存作业策略");
        }
        return location;
    }

    private void putAway(long warehouse, DispatchTask task, StockWork work) {
        InventoryDimension dimension = dimension(warehouse, work.destinationId, work);
        Inventory target = inventory.findByDimension(dimension)
                .orElseGet(() -> new Inventory(null, dimension, 0, 0, 0, 0, 0));
        InventoryChange before = new InventoryChange(target);
        target.increaseOnHand(work.quantity);
        if (target.getInventoryId() == null) {
            inventory.insert(target);
        } else {
            inventory.save(target);
        }
        recordChange(task, target, before, work.type == StockWork.Type.TRANSFER ? "MOVE_IN" : "PUTAWAY");
    }

    private void changeSource(long warehouse, DispatchTask task, StockWork work, String action,
                              java.util.function.Consumer<Inventory> operation) {
        Inventory source = inventory.findByDimension(dimension(warehouse, work.sourceId, work))
                .orElseThrow(() -> new DispatchException(CONFLICT, "源库位批次库存不存在"));
        InventoryChange before = new InventoryChange(source);
        operation.accept(source);
        inventory.save(source);
        recordChange(task, source, before, action);
    }

    private InventoryDimension dimension(long warehouse, long location, StockWork work) {
        return new InventoryDimension(warehouse, location, work.skuId, work.lotNo, InventoryStatus.AVAILABLE);
    }

    private void recordChange(DispatchTask task, Inventory after, InventoryChange before, String action) {
        inventory.appendChange(before.entry(after, action, task.taskKey, task.id, null));
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw new DispatchException(CONFLICT, message);
        }
    }
}
