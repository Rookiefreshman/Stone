package com.stone.wms.infrastructure.mapper;

import com.stone.wms.domain.repository.DispatchHistoryRepository.Row;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

public interface DispatchHistoryMapper {
    // V2 event table + V4 completed_qty. Stable presentation order, not causal ordering.
    @Select("SELECT e.event_key AS eventKey, e.target_state AS targetState, e.reconciled, e.reason, "
            + "e.completed_qty AS completedQty, DATE_FORMAT(e.created_at, '%Y-%m-%dT%H:%i:%s.%f') AS createdAt "
            + "FROM wes_dispatch_event e JOIN wes_dispatch_task t ON t.id=e.task_id "
            + "WHERE t.warehouse_id=#{warehouse} AND t.id=#{task} "
            + "ORDER BY e.created_at,e.event_key LIMIT 101")
    List<Row> events(@Param("warehouse") long warehouse, @Param("task") long task);
}
