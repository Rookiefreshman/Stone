package com.stone.wms.infrastructure.mapper;

import com.stone.wms.domain.repository.CancelReceiptRepository.Receipt;
import com.stone.wms.domain.repository.CancelReceiptRepository.ApprovalUse;
import org.apache.ibatis.annotations.*;

public interface CancelReceiptMapper {
    @Select("SELECT warehouse_id AS warehouseId, operation_id AS operationId, task_id AS taskId,"
        +"request_fingerprint AS requestFingerprint, 'CANCELLED' AS businessState,"
        +"DATE_FORMAT(recorded_at,'%Y-%m-%dT%H:%i:%s.%fZ') AS recordedAt "
        +"FROM wes_cancel_operation_receipt WHERE warehouse_id=#{warehouse} AND operation_id=#{operation}")
    Receipt find(@Param("warehouse") long warehouse,@Param("operation") String operation);
    @Select("SELECT TIMESTAMPDIFF(MICROSECOND,'1970-01-01 00:00:00',UTC_TIMESTAMP(6)) DIV 1000")
    long currentEpochMillis();

    @Insert("INSERT INTO wes_cancel_operation_receipt(warehouse_id,operation_id,task_id,request_fingerprint,recorded_at,"
        +"approval_issuer,approval_grant_id,approval_tenant,approval_requester,approval_reviewer,approval_valid_from_ms,approval_expires_at_ms) "
        +"SELECT #{warehouse},#{operation},#{task},#{fingerprint},UTC_TIMESTAMP(6),"
        +"#{approval.issuer},#{approval.grantId},#{approval.tenant},#{approval.requester},#{approval.reviewer},#{approval.validFromMs},#{approval.expiresAtMs} "
        +"WHERE (TIMESTAMPDIFF(MICROSECOND,'1970-01-01 00:00:00',UTC_TIMESTAMP(6)) DIV 1000) >= #{approval.validFromMs} "
        +"AND (TIMESTAMPDIFF(MICROSECOND,'1970-01-01 00:00:00',UTC_TIMESTAMP(6)) DIV 1000) < #{approval.expiresAtMs}")
    int insert(@Param("warehouse") long warehouse,@Param("operation") String operation,
            @Param("task") long task,@Param("fingerprint") String fingerprint,@Param("approval") ApprovalUse approval);
}
