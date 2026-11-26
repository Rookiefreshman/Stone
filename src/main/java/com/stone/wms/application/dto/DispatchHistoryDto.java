package com.stone.wms.application.dto;

import com.stone.wms.domain.repository.DispatchHistoryRepository.Row;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DispatchHistoryDto {
    public final long warehouseId;
    public final long taskId;
    public final String taskState;
    public final List<Event> events;
    public final boolean truncated;
    public DispatchHistoryDto(long warehouseId,long taskId,String taskState,List<Row> rows) {
        this.warehouseId=warehouseId; this.taskId=taskId; this.taskState=taskState;
        this.truncated=rows.size()>100;
        List<Event> projection=new ArrayList<>();
        for (Row row:rows.subList(0,Math.min(rows.size(),100))) projection.add(new Event(row));
        this.events=Collections.unmodifiableList(projection);
    }
    public static final class Event {
        public final String eventKey, targetState, reason, createdAt;
        public final boolean reconciled;
        public final Integer completedQty;
        Event(Row r) {
            eventKey=r.eventKey; targetState=r.targetState; reconciled=r.reconciled;
            reason=r.reason; completedQty=r.completedQty; createdAt=r.createdAt;
        }
    }
}
