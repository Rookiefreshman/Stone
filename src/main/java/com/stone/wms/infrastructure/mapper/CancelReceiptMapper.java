package com.stone.wms.infrastructure.mapper;

import com.stone.wms.domain.repository.CancelReceiptRepository.Receipt;
import org.apache.ibatis.annotations.*;

public interface CancelReceiptMapper {
    @Select("SELECT warehouse_id AS warehouseId, operation_id AS operationId, task_id AS taskId,"
        +"request_fingerprint AS requestFingerprint, 'CANCELLED' AS businessState,"
        +"DATE_FORMAT(recorded_at,'%Y-%m-%dT%H:%i:%s.%fZ') AS recordedAt "
        +"FROM wes_cancel_operation_receipt WHERE warehouse_id=#{warehouse} AND operation_id=#{operation}")
    Receipt find(@Param("warehouse") long warehouse,@Param("operation") String operation);
    @Insert("INSERT INTO wes_cancel_operation_receipt(warehouse_id,operation_id,task_id,request_fingerprint,recorded_at) "
        +"VALUES(#{warehouse},#{operation},#{task},#{fingerprint},UTC_TIMESTAMP(6))")
    int insert(@Param("warehouse") long warehouse,@Param("operation") String operation,@Param("task") long task,@Param("fingerprint") String fingerprint);
}
