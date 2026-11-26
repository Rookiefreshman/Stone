package com.stone.wms.infrastructure.repository;

import com.stone.wms.domain.repository.CancelReceiptRepository;
import com.stone.wms.infrastructure.mapper.CancelReceiptMapper;
import org.springframework.stereotype.Repository;
@Repository
public class CancelReceiptRepositoryImpl implements CancelReceiptRepository {
    private final CancelReceiptMapper mapper;
    public CancelReceiptRepositoryImpl(CancelReceiptMapper mapper){this.mapper=mapper;}
    public Receipt find(long warehouseId,String operationId){return mapper.find(warehouseId,operationId);}
    public void insert(long warehouseId,String operationId,long taskId,String fingerprint){
        if(mapper.insert(warehouseId,operationId,taskId,fingerprint)!=1)throw new IllegalStateException("RECEIPT_INSERT_FAILED");
    }
}
