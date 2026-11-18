package com.stone.wms.infrastructure.mapper;

import com.stone.wms.domain.model.dispatch.DispatchTask;
import com.stone.wms.domain.repository.DispatchRepository.Capacity;
import com.stone.wms.domain.repository.DispatchRepository.Claim;
import com.stone.wms.domain.repository.DispatchRepository.Event;
import com.stone.wms.infrastructure.entity.DispatchTaskEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;

public interface DispatchMapper {
    @Select("SELECT max_ready, max_active, max_batch FROM wes_dispatch_capacity WHERE warehouse_id = #{warehouse} FOR UPDATE")
    Capacity lockCapacity(@Param("warehouse") long warehouse);

    @Select("SELECT max_active FROM wes_dispatch_device WHERE warehouse_id = #{warehouse} AND device_code = #{device}")
    Integer deviceLimit(@Param("warehouse") long warehouse, @Param("device") String device);

    @Select("SELECT COUNT(*) FROM wes_dispatch_task WHERE warehouse_id = #{warehouse} AND state = 'READY'")
    int readyCount(@Param("warehouse") long warehouse);

    @Select({"<script>", "SELECT COUNT(*) FROM wes_dispatch_task WHERE warehouse_id = #{warehouse}",
             "AND state IN ('IN_FLIGHT', 'UNKNOWN')",
             "<if test='device != null'>AND device_code = #{device}</if>", "</script>"})
    int activeCount(@Param("warehouse") long warehouse, @Param("device") String device);

    String PROJECTION = "SELECT t.id, t.task_key, t.priority, t.state, t.device_code, t.claim_key,"
            + " w.work_type, w.receipt_key, w.sku_id, w.lot_no, w.source_id, w.destination_id, w.quantity"
            + " FROM wes_dispatch_task t LEFT JOIN wms_stock_work w ON w.task_id = t.id ";

    @Select(PROJECTION + "WHERE t.warehouse_id = #{warehouse} AND t.task_key = #{key}")
    DispatchTaskEntity taskByKey(@Param("warehouse") long warehouse, @Param("key") String key);

    @Select(PROJECTION + "WHERE t.warehouse_id = #{warehouse} AND t.id = #{id}")
    DispatchTaskEntity task(@Param("warehouse") long warehouse, @Param("id") long id);

    @Insert("INSERT INTO wes_dispatch_task (warehouse_id, task_key, priority, state) VALUES (#{warehouse}, #{key}, #{priority}, 'READY')")
    void insertTask(@Param("warehouse") long warehouse, @Param("key") String key, @Param("priority") int priority);

    @Select("SELECT device_code, requested_limit FROM wes_claim WHERE warehouse_id = #{warehouse} AND claim_key = #{key}")
    Claim claim(@Param("warehouse") long warehouse, @Param("key") String key);

    @Select(PROJECTION + "WHERE t.warehouse_id = #{warehouse} AND t.claim_key = #{key} ORDER BY t.priority_rank, t.id")
    List<DispatchTaskEntity> claimedTasks(@Param("warehouse") long warehouse, @Param("key") String key);

    @Select("SELECT id FROM wes_dispatch_task WHERE warehouse_id = #{warehouse} AND state = 'READY'"
            + " ORDER BY priority_rank, id LIMIT #{limit}")
    List<Long> readyIds(@Param("warehouse") long warehouse, @Param("limit") int limit);

    @Insert("INSERT INTO wes_claim (warehouse_id, claim_key, device_code, requested_limit) VALUES (#{warehouse}, #{key}, #{device}, #{limit})")
    void insertClaim(@Param("warehouse") long warehouse, @Param("key") String key, @Param("device") String device,
                     @Param("limit") int limit);

    @Update({"<script>", "UPDATE wes_dispatch_task SET state = 'IN_FLIGHT', claim_key = #{key}, device_code = #{device}",
            "WHERE warehouse_id = #{warehouse} AND state = 'READY' AND id IN",
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>", "</script>"})
    int claimTasks(@Param("warehouse") long warehouse, @Param("ids") List<Long> ids, @Param("key") String key,
                   @Param("device") String device);

    @Select("SELECT target_state, reconciled, reason, completed_qty FROM wes_dispatch_event WHERE task_id = #{id} AND event_key = #{key}")
    Event event(@Param("id") long id, @Param("key") String key);

    @Insert("INSERT INTO wes_dispatch_event (task_id, event_key, target_state, reconciled, reason, completed_qty)"
            + " VALUES (#{id}, #{key}, #{state}, #{reconciled}, #{reason,jdbcType=VARCHAR}, #{completedQty,jdbcType=INTEGER})")
    void insertEvent(@Param("id") long id, @Param("key") String key, @Param("state") DispatchTask.State state,
                     @Param("reconciled") boolean reconciled, @Param("reason") String reason,
                     @Param("completedQty") Integer completedQty);

    @Update("UPDATE wes_dispatch_task SET state = #{to} WHERE warehouse_id = #{warehouse} AND id = #{id} AND state = #{from}")
    int updateTask(@Param("warehouse") long warehouse, @Param("id") long id, @Param("from") DispatchTask.State from,
                   @Param("to") DispatchTask.State to);
}
