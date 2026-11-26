package com.stone.wms.infrastructure.repository;

import com.stone.wms.domain.repository.CancelReceiptRepository;
import com.stone.wms.infrastructure.mapper.CancelReceiptMapper;
import org.springframework.stereotype.Repository;
@Repository
public class CancelReceiptRepositoryImpl implements CancelReceiptRepository {
    private final CancelReceiptMapper mapper;
    public CancelReceiptRepositoryImpl(CancelReceiptMapper mapper){this.mapper=mapper;}
    public Receipt find(long warehouseId,String operationId){return mapper.find(warehouseId,operationId);}
    public long currentEpochMillis(){return mapper.currentEpochMillis();}
    public void insert(long warehouseId,String operationId,long taskId,String fingerprint,ApprovalUse approval){
        if(approval==null)throw new SecurityException("VERIFIED_APPROVAL_REQUIRED");
        // Unique grant constraint is global across warehouses. Do not catch and
        // continue on a duplicate key: the entire enclosing business transaction must roll back.
        if(mapper.insert(warehouseId,operationId,taskId,fingerprint,approval)!=1)
            throw new SecurityException("APPROVAL_EXPIRED_AT_RECEIPT_INSERT");
    }
}
