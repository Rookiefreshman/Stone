package com.stone.wms.domain.repository;

import com.stone.wms.domain.model.dispatch.DispatchTask;
import java.util.List;

public interface DispatchRepository {
    class Capacity {
        public int maxReady;
        public int maxActive;
        public int maxBatch;
    }

    class Claim {
        public String deviceCode;
        public int requestedLimit;
    }

    class Event {
        public DispatchTask.State targetState;
        public boolean reconciled;
        public String reason;
    }

    Capacity lockCapacity(long warehouseId);
    Integer deviceLimit(long warehouseId, String deviceCode);
    int readyCount(long warehouseId);
    int activeCount(long warehouseId, String deviceCode);
    DispatchTask taskByKey(long warehouseId, String taskKey);
    DispatchTask task(long warehouseId, long taskId);
    void insertTask(long warehouseId, String taskKey, int priority);
    Claim claim(long warehouseId, String claimKey);
    List<DispatchTask> claimedTasks(long warehouseId, String claimKey);
    List<Long> readyIds(long warehouseId, int limit);
    void insertClaim(long warehouseId, String claimKey, String deviceCode, int limit);
    int claimTasks(long warehouseId, List<Long> ids, String claimKey, String deviceCode);
    Event event(long taskId, String eventKey);
    void insertEvent(long taskId, String eventKey, DispatchTask.State target, boolean reconciled, String reason);
    int updateTask(long warehouseId, long taskId, DispatchTask.State from, DispatchTask.State to);
}
