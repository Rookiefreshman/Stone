package com.stone.wms.infrastructure.mapper;

import com.stone.wms.domain.repository.DispatchDiagnosticRepository.Row;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface DispatchDiagnosticMapper {
    // Single non-locking read. Terminal tasks never consume slots.
    // Start from registered device, so an unregistered device is not an empty queue.
    @Select("SELECT c.warehouse_id, d.device_code, c.max_ready, c.max_active, c.max_batch,"
        + " d.max_active AS device_max_active,"
        + " SUM(CASE WHEN t.state='READY' THEN 1 ELSE 0 END) AS ready,"
        + " SUM(CASE WHEN t.state='IN_FLIGHT' THEN 1 ELSE 0 END) AS in_flight,"
        + " SUM(CASE WHEN t.state='UNKNOWN' THEN 1 ELSE 0 END) AS unknown,"
        + " SUM(CASE WHEN t.state='IN_FLIGHT' AND t.device_code=d.device_code THEN 1 ELSE 0 END) AS device_in_flight,"
        + " SUM(CASE WHEN t.state='UNKNOWN' AND t.device_code=d.device_code THEN 1 ELSE 0 END) AS device_unknown"
        + " FROM wes_dispatch_capacity c"
        + " JOIN wes_dispatch_device d ON d.warehouse_id=c.warehouse_id AND d.device_code=#{device}"
        + " LEFT JOIN wes_dispatch_task t ON t.warehouse_id=c.warehouse_id AND t.state IN ('READY','IN_FLIGHT','UNKNOWN')"
        + " WHERE c.warehouse_id=#{warehouse}"
        + " GROUP BY c.warehouse_id,d.device_code,c.max_ready,c.max_active,c.max_batch,d.max_active")
    Row snapshot(@Param("warehouse") long warehouseId,@Param("device") String deviceCode);
}
