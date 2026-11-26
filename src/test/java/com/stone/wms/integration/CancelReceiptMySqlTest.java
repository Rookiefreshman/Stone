package com.stone.wms.integration;

import com.stone.wms.application.service.*;
import com.stone.wms.application.dto.DispatchTaskDto;
import com.stone.wms.domain.model.inventory.StockWork;
import com.stone.wms.domain.repository.CancelReceiptRepository;
import com.stone.wms.startup.WmsApplication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.UUID;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@EnabledIfEnvironmentVariable(named="STONE_TEST_MYSQL_URL",matches=".+")
@SpringBootTest(classes=WmsApplication.class,properties={
    "spring.datasource.url=${STONE_TEST_MYSQL_URL}",
    "spring.datasource.username=${STONE_TEST_MYSQL_USER}",
    "spring.datasource.password=${STONE_TEST_MYSQL_PASSWORD}"
})
class CancelReceiptMySqlTest {
    @Autowired JdbcTemplate sql;
    @Autowired DispatchApplicationService dispatch;
    @Autowired CancelOperationService service;
    @MockBean CancelAuthorization policy; // Test-only authorizer. Never used in production.
    @SpyBean CancelReceiptRepository receipts;
    private CancelCommand fixture(long warehouse) {
        sql.update("INSERT INTO wes_dispatch_capacity VALUES(?,20,8,8)",warehouse);
        sql.update("INSERT INTO wms_work_location(warehouse_id,location_id,max_units,mix_policy) VALUES(?,1,20,'ANY'),(?,2,20,'ANY')",warehouse,warehouse);
        sql.update("INSERT INTO wms_inventory(id,warehouse_id,location_id,sku_id,lot_no,inventory_status,on_hand_qty,available_qty) VALUES(?,?,1,11,'B1','AVAILABLE',10,10)",warehouse,warehouse);
        DispatchTaskDto task=dispatch.submitStockWork(warehouse,"receipt-test",10,new StockWork(StockWork.Type.TRANSFER,null,11,"B1",1L,2,6));
        return new CancelCommand(UUID.randomUUID().toString(),warehouse,task.id,"test","requester","receipt-test","TRANSFER",11,"B1",1L,2,6,"test-only","aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
    }
    @Test void successfulReceiptReplayAndDifferentOperationCannotClaimCredit(){
        CancelCommand c=fixture(99101);
        assertEquals(c.fingerprint(),service.submit(c).requestFingerprint);
        assertEquals(c.fingerprint(),service.submit(c).requestFingerprint);
        assertEquals(c.fingerprint(),service.query(c.warehouseId,c.operationId).requestFingerprint);
        assertNull(service.query(99102,c.operationId));
        assertEquals(0,sql.queryForObject("SELECT reserved_qty FROM wms_inventory WHERE id=99101",Integer.class));
        assertEquals(1,sql.queryForObject("SELECT COUNT(*) FROM wms_inventory_transaction WHERE work_task_id=? AND biz_type='MOVE_RELEASE'",Integer.class,c.taskId));
        CancelCommand other=new CancelCommand(UUID.randomUUID().toString(),c.warehouseId,c.taskId,c.tenant,c.actor,c.taskKey,c.workType,c.skuId,c.lotNo,c.sourceId,c.destinationId,c.quantity,c.reason,c.approvalHash);
        assertThrows(IllegalStateException.class,()->service.submit(other));
        assertNull(service.query(other.warehouseId,other.operationId));
    }
    @Test void receiptInsertFailureRollsBackCancellationAndReservation(){
        CancelCommand c=fixture(99103);
        doThrow(new IllegalStateException("injected receipt write failure")).when(receipts).insert(c.warehouseId,c.operationId,c.taskId,c.fingerprint());
        assertThrows(IllegalStateException.class,()->service.submit(c));
        assertEquals("READY",dispatch.stockWorkDetail(c.warehouseId,c.taskId).task.state);
        assertEquals(6,sql.queryForObject("SELECT reserved_qty FROM wms_inventory WHERE id=99103",Integer.class));
        assertEquals(0,sql.queryForObject("SELECT COUNT(*) FROM wms_inventory_transaction WHERE work_task_id=? AND biz_type='MOVE_RELEASE'",Integer.class,c.taskId));
        assertNull(service.query(c.warehouseId,c.operationId));
    }
    @Test void duplicateConcurrentOperationReturnsOneReceipt() throws Exception {
        CancelCommand c=fixture(99104);
        ExecutorService pool=Executors.newFixedThreadPool(2);
        try {
            CountDownLatch gate=new CountDownLatch(1);
            Callable<String> run=()->{gate.await();return service.submit(c).requestFingerprint;};
            Future<String> a=pool.submit(run), b=pool.submit(run);gate.countDown();
            assertEquals(c.fingerprint(),a.get(20,TimeUnit.SECONDS));assertEquals(c.fingerprint(),b.get(20,TimeUnit.SECONDS));
            assertEquals(1,sql.queryForObject("SELECT COUNT(*) FROM wes_cancel_operation_receipt WHERE warehouse_id=99104",Integer.class));
            assertEquals(1,sql.queryForObject("SELECT COUNT(*) FROM wms_inventory_transaction WHERE work_task_id=? AND biz_type='MOVE_RELEASE'",Integer.class,c.taskId));
        }finally{pool.shutdownNow();}
    }
}
