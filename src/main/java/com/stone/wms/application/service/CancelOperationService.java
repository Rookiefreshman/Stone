package com.stone.wms.application.service;

import com.stone.wms.domain.model.dispatch.DispatchTask;
import com.stone.wms.domain.repository.DispatchRepository;
import com.stone.wms.domain.repository.CancelReceiptRepository;
import com.stone.wms.domain.repository.CancelReceiptRepository.Receipt;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

/** Internal service only: no controller exposes submit. Default deny without policy bean. */
@Service
public class CancelOperationService {
    private final DispatchRepository tasks;
    private final DispatchApplicationService dispatch;
    private final CancelReceiptRepository receipts;
    private final ObjectProvider<CancelAuthorization> policy;
    public CancelOperationService(DispatchRepository tasks,DispatchApplicationService dispatch,
            CancelReceiptRepository receipts,ObjectProvider<CancelAuthorization> policy) {
        this.tasks=tasks;this.dispatch=dispatch;this.receipts=receipts;this.policy=policy;
    }
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public Receipt submit(CancelCommand c) {
        CancelAuthorization auth=policy.getIfAvailable();
        if(auth==null)throw new SecurityException("CANCEL_POLICY_UNCONFIGURED");
        // Check current authorization before observing warehouse or receipt existence.
        auth.requireAuthorized(c,true);
        if(tasks.lockCapacity(c.warehouseId)==null)throw new IllegalStateException("WAREHOUSE_NOT_FOUND");
        Receipt old=receipts.find(c.warehouseId,c.operationId);
        if(old!=null) {
            auth.requireAuthorized(c,true);
            if(old.taskId!=c.taskId || !c.fingerprint().equals(old.requestFingerprint))
                throw new IllegalStateException("OPERATION_ID_CONFLICT");
            return old;
        }
        auth.requireAuthorized(c,false);
        DispatchTask task=tasks.task(c.warehouseId,c.taskId);
        if(!c.matches(task) || task.state!=DispatchTask.State.READY)
            throw new IllegalStateException("TASK_NOT_READY_OR_CHANGED");
        // Reuse existing domain cancellation, which joins this transaction and locks the same warehouse.
        // Refuse to attribute a pre-existing CANCELLED state to this operation.
        if(!"CANCELLED".equals(dispatch.cancelStockWork(c.warehouseId,c.taskId).state))
            throw new IllegalStateException("CANCEL_NOT_COMMITTED");
        receipts.insert(c.warehouseId,c.operationId,c.taskId,c.fingerprint());
        Receipt result=receipts.find(c.warehouseId,c.operationId);
        if(result==null || !c.fingerprint().equals(result.requestFingerprint))throw new IllegalStateException("RECEIPT_MISSING");
        return result;
    }
    @Transactional(readOnly=true)
    public Receipt query(long warehouseId,String operationId){return receipts.find(warehouseId,operationId);}
}
