package com.stone.wms.application.service;

import com.stone.wms.application.dto.DispatchHistoryDto;
import com.stone.wms.domain.exception.DispatchException;
import com.stone.wms.domain.model.dispatch.DispatchTask;
import com.stone.wms.domain.repository.DispatchRepository;
import com.stone.wms.domain.repository.DispatchHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DispatchHistoryQueryService {
    private final DispatchRepository tasks;
    private final DispatchHistoryRepository history;
    public DispatchHistoryQueryService(DispatchRepository tasks,DispatchHistoryRepository history) {
        this.tasks=tasks; this.history=history;
    }
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public DispatchHistoryDto query(long warehouseId,long taskId) {
        DispatchTask task=tasks.task(warehouseId,taskId);
        if (task==null || task.work==null) throw new DispatchException(DispatchException.Code.NOT_FOUND,"库存作业不存在");
        return new DispatchHistoryDto(warehouseId,taskId,task.state.name(),history.events(warehouseId,taskId));
    }
}
