package com.stone.warehouse.application;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.stereotype.Service;

import com.stone.warehouse.domain.WarehouseRepository;
import static com.stone.warehouse.domain.WarehouseModel.*;
import static com.stone.warehouse.domain.WarehouseProblem.*;

@Service
class InventoryService {
    private final WarehouseRepository repository;

    InventoryService(WarehouseRepository repository) {
        this.repository = repository;
    }

    void ensureEligibleBatch(Task task) {
        Lot lot = found(repository.lotById(task.lotId), "商品批次");
        require(lot.expiresOn == null || !lot.expiresOn.isBefore(LocalDate.now(ZoneOffset.UTC)),
                "BATCH_EXPIRED", "该批次已过期，请核对实物并走退拣或取消流程");
    }

    void ensureUnfrozen(String warehouseId, String locationId, String lotId) {
        require(repository.activeCount(warehouseId, locationId, lotId) == 0,
                "COUNT_LOCKED", "该库位批次正处于盘点冻结");
    }

    void reserve(Document document, Line line) {
        long remaining = line.quantity;
        for (Stock stock : repository.candidates(line.skuId, line.lotId, line.sourceId,
                LocalDate.now(ZoneOffset.UTC))) {
            if (repository.activeCount(document.warehouseId, stock.locationId, stock.lotId) > 0) {
                continue;
            }
            if (line.destinationId != null) {
                ensureUnfrozen(document.warehouseId, line.destinationId, stock.lotId);
            }
            long quantity = Math.min(remaining, stock.available());
            Task task = createTask(document, line, stock.lotId, stock.locationId, quantity);
            post(task, task.sourceId, 0, quantity, "RESERVE");
            remaining -= quantity;
            if (remaining == 0) {
                return;
            }
        }
        require(false, "INSUFFICIENT_STOCK", "第 " + line.lineNo + " 行的合格可用库存不足");
    }

    Task createTask(Document document, Line line, String lotId, String sourceId, long quantity) {
        Task task = new Task();
        task.id = UUID.randomUUID().toString();
        task.warehouseId = document.warehouseId;
        task.documentId = document.id;
        task.lineId = line.id;
        task.lotId = lotId;
        task.sourceId = sourceId;
        task.destinationId = line.destinationId;
        task.quantity = quantity;
        task.state = TaskState.READY;
        repository.insertTask(task);
        return task;
    }

    void post(Task task, String locationId, long physical, long reserved, String reason) {
        Stock stock = repository.stock(locationId, task.lotId);
        if (stock == null) {
            stock = new Stock();
            stock.locationId = locationId;
            stock.lotId = task.lotId;
            stock.apply(physical, reserved);
            repository.insertStock(stock);
        } else {
            stock.apply(physical, reserved);
            repository.updateStock(stock);
        }
        Ledger ledger = new Ledger();
        ledger.id = UUID.randomUUID().toString();
        ledger.warehouseId = task.warehouseId;
        ledger.taskId = task.id;
        ledger.locationId = locationId;
        ledger.lotId = task.lotId;
        ledger.reason = reason;
        ledger.deltaOnHand = physical;
        ledger.deltaReserved = reserved;
        repository.insertLedger(ledger);
    }
}
