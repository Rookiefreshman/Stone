package com.stone.wms.domain.repository;

public interface CancelReceiptRepository {
    class Receipt {
        public String operationId,requestFingerprint,businessState,recordedAt;
        public long warehouseId,taskId;
    }
    Receipt find(long warehouseId,String operationId);
    void insert(long warehouseId,String operationId,long taskId,String fingerprint);
}
