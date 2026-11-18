package com.stone.wms.infrastructure.repository;

import com.stone.wms.domain.model.dispatch.DispatchTask;
import com.stone.wms.domain.repository.DispatchRepository;
import com.stone.wms.infrastructure.entity.DispatchTaskEntity;
import com.stone.wms.infrastructure.mapper.DispatchMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
public class DispatchRepositoryImpl implements DispatchRepository {
    private final DispatchMapper mapper;

    public DispatchRepositoryImpl(DispatchMapper mapper) {
        this.mapper = mapper;
    }

    private static DispatchTask toDomain(DispatchTaskEntity row) {
        return row == null ? null : new DispatchTask(row.id, row.taskKey, row.priority, row.state, row.deviceCode, row.claimKey);
    }

    @Override public Capacity lockCapacity(long warehouseId) { return mapper.lockCapacity(warehouseId); }
    @Override public Integer deviceLimit(long warehouseId, String deviceCode) { return mapper.deviceLimit(warehouseId, deviceCode); }
    @Override public int readyCount(long warehouseId) { return mapper.readyCount(warehouseId); }
    @Override public int activeCount(long warehouseId, String deviceCode) { return mapper.activeCount(warehouseId, deviceCode); }
    @Override public DispatchTask taskByKey(long warehouseId, String taskKey) { return toDomain(mapper.taskByKey(warehouseId, taskKey)); }
    @Override public DispatchTask task(long warehouseId, long taskId) { return toDomain(mapper.task(warehouseId, taskId)); }
    @Override public void insertTask(long warehouseId, String taskKey, int priority) { mapper.insertTask(warehouseId, taskKey, priority); }
    @Override public Claim claim(long warehouseId, String claimKey) { return mapper.claim(warehouseId, claimKey); }

    @Override
    public List<DispatchTask> claimedTasks(long warehouseId, String claimKey) {
        return mapper.claimedTasks(warehouseId, claimKey).stream().map(DispatchRepositoryImpl::toDomain).collect(Collectors.toList());
    }

    @Override public List<Long> readyIds(long warehouseId, int limit) { return mapper.readyIds(warehouseId, limit); }
    @Override public void insertClaim(long warehouseId, String claimKey, String deviceCode, int limit) {
        mapper.insertClaim(warehouseId, claimKey, deviceCode, limit);
    }

    @Override public int claimTasks(long warehouseId, List<Long> ids, String claimKey, String deviceCode) {
        return mapper.claimTasks(warehouseId, ids, claimKey, deviceCode);
    }

    @Override public Event event(long taskId, String eventKey) { return mapper.event(taskId, eventKey); }
    @Override public void insertEvent(long taskId, String eventKey, DispatchTask.State target, boolean reconciled, String reason) {
        mapper.insertEvent(taskId, eventKey, target, reconciled, reason);
    }

    @Override public int updateTask(long warehouseId, long taskId, DispatchTask.State from, DispatchTask.State to) {
        return mapper.updateTask(warehouseId, taskId, from, to);
    }
}
