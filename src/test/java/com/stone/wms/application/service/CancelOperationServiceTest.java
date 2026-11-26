package com.stone.wms.application.service;
import com.stone.wms.domain.model.dispatch.DispatchTask;
import com.stone.wms.domain.model.inventory.StockWork;
import com.stone.wms.domain.repository.*;
import com.stone.wms.domain.repository.CancelReceiptRepository.Receipt;
import com.stone.wms.interfaces.controller.CancelReceiptQueryController;
import org.springframework.beans.factory.ObjectProvider;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CancelOperationServiceTest {
    private CancelCommand command(){return new CancelCommand("00000000-0000-0000-0000-000000000001",1,101,"demo","requester","task-101","TRANSFER",8,"L01",10L,20,12,"重复申请","aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");}
    @Test void crossLanguageFingerprint(){
        assertEquals("f384e17fa1a042bfb31974d2e40c8ade060258660a4bd2102da9d70b20fd68fe",command().fingerprint());
    }
    @SuppressWarnings("unchecked")
    @Test void defaultDenyBeforeDatabase(){
        DispatchRepository tasks=mock(DispatchRepository.class);DispatchApplicationService dispatch=mock(DispatchApplicationService.class);
        CancelReceiptRepository receipts=mock(CancelReceiptRepository.class);ObjectProvider<CancelAuthorization> policy=mock(ObjectProvider.class);
        assertThrows(SecurityException.class,()->new CancelOperationService(tasks,dispatch,receipts,policy).submit(command()));
        verifyNoInteractions(tasks,dispatch,receipts);
    }
    @SuppressWarnings("unchecked")
    @Test void replayRequiresSameFingerprintAndDoesNotCancel(){
        DispatchRepository tasks=mock(DispatchRepository.class);DispatchApplicationService dispatch=mock(DispatchApplicationService.class);
        CancelReceiptRepository receipts=mock(CancelReceiptRepository.class);ObjectProvider<CancelAuthorization> policy=mock(ObjectProvider.class);
        when(policy.getIfAvailable()).thenReturn(mock(CancelAuthorization.class));when(tasks.lockCapacity(1)).thenReturn(new DispatchRepository.Capacity());
        Receipt old=new Receipt();old.taskId=101;old.requestFingerprint=command().fingerprint();
        when(receipts.find(1,command().operationId)).thenReturn(old);
        CancelOperationService service=new CancelOperationService(tasks,dispatch,receipts,policy);
        assertSame(old,service.submit(command()));
        old.requestFingerprint="different";assertThrows(IllegalStateException.class,()->service.submit(command()));verifyNoInteractions(dispatch);
    }
    @SuppressWarnings("unchecked")
    @Test void preexistingCancelledCannotBeAttributed(){
        DispatchRepository tasks=mock(DispatchRepository.class);DispatchApplicationService dispatch=mock(DispatchApplicationService.class);
        CancelReceiptRepository receipts=mock(CancelReceiptRepository.class);ObjectProvider<CancelAuthorization> policy=mock(ObjectProvider.class);
        when(policy.getIfAvailable()).thenReturn(mock(CancelAuthorization.class));when(tasks.lockCapacity(1)).thenReturn(new DispatchRepository.Capacity());
        when(tasks.task(1,101)).thenReturn(new DispatchTask(101,"task-101",10,DispatchTask.State.CANCELLED,null,null,new StockWork(StockWork.Type.TRANSFER,null,8,"L01",10L,20,12)));
        assertThrows(IllegalStateException.class,()->new CancelOperationService(tasks,dispatch,receipts,policy).submit(command()));
        verifyNoInteractions(dispatch);verify(receipts,never()).insert(anyLong(),anyString(),anyLong(),anyString());
    }
    @Test void queryScopeAndMissing(){
        CancelOperationService service=mock(CancelOperationService.class);String token="unit-test-read-only-123456789";
        CancelReceiptQueryController c=new CancelReceiptQueryController(service,token,"1");
        assertEquals(401,c.query(1,command().operationId,null).getStatusCodeValue());
        assertEquals(403,c.query(2,command().operationId,"Bearer "+token).getStatusCodeValue());
        verifyNoInteractions(service);
        assertEquals(404,c.query(1,command().operationId,"Bearer "+token).getStatusCodeValue());
    }
}
