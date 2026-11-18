package com.stone.wms.application.service;

import com.stone.wms.application.dto.DispatchTaskDto;
import com.stone.wms.application.dto.StockWorkDetailDto;
import com.stone.wms.domain.exception.DispatchException;
import com.stone.wms.domain.model.dispatch.DispatchTask;
import com.stone.wms.domain.model.dispatch.DispatchTask.State;
import com.stone.wms.domain.model.inventory.StockWork;
import com.stone.wms.domain.repository.DispatchRepository;
import com.stone.wms.domain.repository.DispatchRepository.Capacity;
import com.stone.wms.domain.repository.DispatchRepository.Claim;
import com.stone.wms.domain.repository.DispatchRepository.Event;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.stone.wms.domain.exception.DispatchException.Code.*;

@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class DispatchApplicationService {
    private final DispatchRepository repository;

    private final StockWorkService stockWork;

    public DispatchApplicationService(DispatchRepository repository, StockWorkService stockWork) {
        this.repository = repository;
        this.stockWork = stockWork;
    }

    public DispatchTaskDto enqueue(long warehouseId, String taskKey, int priority) {
        return submit(warehouseId, taskKey, priority, null);
    }

    public void registerReceipt(long warehouseId, String key, long sku, String lot, int quantity) {
        lock(warehouseId);
        stockWork.registerReceipt(warehouseId, key, sku, lot, quantity);
    }

    public DispatchTaskDto submitStockWork(long warehouseId, String taskKey, int priority, StockWork work) {
        return submit(warehouseId, taskKey, priority, Objects.requireNonNull(work));
    }

    private DispatchTaskDto submit(long warehouseId, String taskKey, int priority, StockWork work) {
        Capacity capacity = lock(warehouseId);
        DispatchTask prior = repository.taskByKey(warehouseId, taskKey);
        if (prior != null) {
            require(prior.priority == priority && (prior.work == null ? work == null : work != null && prior.work.sameAs(work)),
                    "相同任务键的优先级或库存作业内容不一致");
            return new DispatchTaskDto(prior);
        }
        if (repository.readyCount(warehouseId) >= capacity.maxReady) {
            throw new DispatchException(QUEUE_FULL, "待派队列已满，请退避后重试");
        }
        repository.insertTask(warehouseId, taskKey, priority);
        if (work != null) {
            stockWork.reserve(warehouseId, repository.taskByKey(warehouseId, taskKey), work);
        }
        return new DispatchTaskDto(repository.taskByKey(warehouseId, taskKey));
    }

    public List<DispatchTaskDto> claim(long warehouseId, String deviceCode, String claimKey, int requestedLimit) {
        Capacity capacity = lock(warehouseId);
        Claim prior = repository.claim(warehouseId, claimKey);
        if (prior != null) {
            require(prior.deviceCode.equals(deviceCode) && prior.requestedLimit == requestedLimit,
                    "相同领取键的设备或请求数量不一致");
            return claimed(warehouseId, claimKey);
        }
        Integer deviceLimit = repository.deviceLimit(warehouseId, deviceCode);
        if (deviceLimit == null) {
            throw new DispatchException(NOT_FOUND, "设备未登记");
        }
        int warehouseSlots = capacity.maxActive - repository.activeCount(warehouseId, null);
        int deviceSlots = deviceLimit - repository.activeCount(warehouseId, deviceCode);
        int limit = Math.min(Math.min(capacity.maxBatch, requestedLimit), Math.min(warehouseSlots, deviceSlots));
        if (limit <= 0) {
            return Collections.emptyList();
        }
        List<Long> ids = repository.readyIds(warehouseId, limit);
        if (ids.isEmpty()) {
            return Collections.emptyList();
        }
        repository.insertClaim(warehouseId, claimKey, deviceCode, requestedLimit);
        require(repository.claimTasks(warehouseId, ids, claimKey, deviceCode) == ids.size(), "任务领取状态发生变化");
        return claimed(warehouseId, claimKey);
    }

    public DispatchTaskDto report(long warehouseId, long taskId, String deviceCode, String claimKey,
                                  String eventKey, State next, boolean reconciled, String reason) {
        return report(warehouseId, taskId, deviceCode, claimKey, eventKey, next, reconciled, reason, null);
    }

    public DispatchTaskDto report(long warehouseId, long taskId, String deviceCode, String claimKey,
                                  String eventKey, State next, boolean reconciled, String reason, Integer completedQty) {
        lock(warehouseId);
        DispatchTask task = repository.task(warehouseId, taskId);
        if (task == null) {
            throw new DispatchException(NOT_FOUND, "任务不存在");
        }
        require(deviceCode.equals(task.deviceCode) && claimKey.equals(task.claimKey), "设备或领取键与当前任务不匹配");
        Event prior = repository.event(taskId, eventKey);
        if (prior != null) {
            require(prior.targetState == next && prior.reconciled == reconciled && Objects.equals(prior.reason, reason)
                            && Objects.equals(prior.completedQty, completedQty),
                    "相同回执键的内容不一致");
            return new DispatchTaskDto(task);
        }
        require(task.state.allows(next, reconciled), "任务状态不允许此回执");
        require((next == State.SUCCEEDED && !reconciled) || (reason != null && !reason.trim().isEmpty()),
                "异常或核对回执必须填写原因");
        stockWork.finish(warehouseId, task, next, completedQty);
        repository.insertEvent(taskId, eventKey, next, reconciled, reason, completedQty);
        require(repository.updateTask(warehouseId, taskId, task.state, next) == 1, "任务状态发生变化");
        return new DispatchTaskDto(repository.task(warehouseId, taskId));
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public StockWorkDetailDto stockWorkDetail(long warehouseId, long taskId) {
        return stockWork.detail(warehouseId, requireStockWork(warehouseId, taskId));
    }

    public DispatchTaskDto cancelStockWork(long warehouseId, long taskId) {
        lock(warehouseId);
        DispatchTask task = requireStockWork(warehouseId, taskId);
        if (task.state != State.CANCELLED) {
            require(task.state == State.READY, "仅未下发的库存任务可取消");
            stockWork.finish(warehouseId, task, State.CANCELLED, null);
            require(repository.updateTask(warehouseId, taskId, task.state, State.CANCELLED) == 1, "任务状态发生变化");
            task = repository.task(warehouseId, taskId);
        }
        return new DispatchTaskDto(task);
    }

    private DispatchTask requireStockWork(long warehouseId, long taskId) {
        DispatchTask task = repository.task(warehouseId, taskId);
        if (task == null || task.work == null) {
            throw new DispatchException(NOT_FOUND, "库存作业不存在");
        }
        return task;
    }

    public boolean freezeLocation(long warehouseId, long locationId, String key, String reason) {
        return stockWork.freeze(warehouseId, locationId, key, reason);
    }

    public boolean unfreezeLocation(long warehouseId, long locationId, String key) {
        return stockWork.unfreeze(warehouseId, locationId, key);
    }

    private List<DispatchTaskDto> claimed(long warehouseId, String key) {
        return repository.claimedTasks(warehouseId, key).stream().map(DispatchTaskDto::new).collect(Collectors.toList());
    }

    private Capacity lock(long warehouseId) {
        Capacity capacity = repository.lockCapacity(warehouseId);
        if (capacity == null) {
            throw new DispatchException(NOT_FOUND, "仓库尚未配置调度容量");
        }
        return capacity;
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw new DispatchException(CONFLICT, message);
        }
    }
}
