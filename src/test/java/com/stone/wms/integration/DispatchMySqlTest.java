package com.stone.wms.integration;

import com.stone.wms.application.dto.AllocateOutboundCommand;
import com.stone.wms.application.dto.DispatchTaskDto;
import com.stone.wms.application.dto.OutboundOrderDto;
import com.stone.wms.application.service.DispatchApplicationService;
import com.stone.wms.application.service.OutboundAllocationApplicationService;
import com.stone.wms.domain.exception.DispatchException;
import com.stone.wms.domain.exception.DomainException;
import com.stone.wms.domain.model.inventory.StockWork;
import com.stone.wms.startup.WmsApplication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;

import static com.stone.wms.domain.exception.DispatchException.Code.QUEUE_FULL;
import static com.stone.wms.domain.model.dispatch.DispatchTask.State.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@EnabledIfEnvironmentVariable(named = "STONE_TEST_MYSQL_URL", matches = ".+")
@SpringBootTest(classes = WmsApplication.class, properties = {
        "spring.datasource.url=${STONE_TEST_MYSQL_URL}",
        "spring.datasource.username=${STONE_TEST_MYSQL_USER}",
        "spring.datasource.password=${STONE_TEST_MYSQL_PASSWORD}"
})
@AutoConfigureMockMvc
class DispatchMySqlTest {
    @Autowired DispatchApplicationService dispatch;
    @Autowired OutboundAllocationApplicationService outbound;
    @Autowired JdbcTemplate sql;
    @Autowired MockMvc http;

    @Test
    void concurrentConsumersRespectWarehouseDeviceAndBatchLimits() throws Exception {
        warehouse(201, 20, 5, 2);
        device(201, "a", 3);
        device(201, "b", 3);
        for (int i = 0; i < 10; i++) {
            dispatch.enqueue(201, "job-" + i, i);
        }
        List<List<DispatchTaskDto>> batches = compete(16, i ->
                dispatch.claim(201, i % 2 == 0 ? "a" : "b", "claim-" + i, 10));
        Set<Long> ids = new HashSet<>();
        int a = 0;
        int b = 0;
        for (List<DispatchTaskDto> batch : batches) {
            assertThat(batch).hasSizeLessThanOrEqualTo(2);
            for (DispatchTaskDto task : batch) {
                assertThat(ids.add(task.id)).isTrue();
                if ("a".equals(task.deviceCode)) {
                    a++;
                } else {
                    b++;
                }
            }
        }
        assertThat(ids).hasSize(5);
        assertThat(a).isLessThanOrEqualTo(3);
        assertThat(b).isLessThanOrEqualTo(3);
        assertThat(sql.queryForList("SELECT priority FROM wes_dispatch_task WHERE warehouse_id=201 AND state='IN_FLIGHT'", Integer.class))
                .containsExactlyInAnyOrder(9, 8, 7, 6, 5);
    }

    @Test
    void concurrentProducersAndReceiptsPreserveKeysAndCapacity() throws Exception {
        warehouse(202, 1, 1, 1);
        device(202, "station", 1);
        assertThat(compete(6, i -> dispatch.enqueue(202, "same", 10)))
                .extracting(task -> task.id).containsOnly(dispatch.enqueue(202, "same", 10).id);
        assertThatThrownBy(() -> dispatch.enqueue(202, "excess", 10))
                .isInstanceOfSatisfying(DispatchException.class, failure -> assertThat(failure.getCode()).isEqualTo(QUEUE_FULL));
        assertThatThrownBy(() -> dispatch.enqueue(202, "same", 11)).isInstanceOf(DispatchException.class);
        long id = dispatch.claim(202, "station", "claim", 1).get(0).id;
        assertThat(dispatch.claim(202, "station", "claim", 1)).extracting(task -> task.id).containsExactly(id);
        assertThatThrownBy(() -> dispatch.claim(202, "station", "claim", 2)).isInstanceOf(DispatchException.class);
        dispatch.enqueue(202, "waiting", 0);
        assertThat(dispatch.report(202, id, "station", "claim", "lost", UNKNOWN, false, "响应超时").state).isEqualTo("UNKNOWN");
        assertThat(dispatch.claim(202, "station", "other-claim", 1)).isEmpty();
        assertThatThrownBy(() -> dispatch.report(202, id, "station", "claim", "bad", SUCCEEDED, false, null))
                .isInstanceOf(DispatchException.class);
        assertThat(compete(5, i -> dispatch.report(202, id, "station", "claim", "checked", SUCCEEDED, true, "已核对实物")))
                .extracting(task -> task.state).containsOnly("SUCCEEDED");
        assertThatThrownBy(() -> dispatch.report(202, id, "station", "claim", "checked", FAILED, true, "改写"))
                .isInstanceOf(DispatchException.class);
        assertThat(dispatch.claim(202, "station", "other-claim", 1)).extracting(task -> task.taskKey).containsExactly("waiting");
    }

    @Test
    void concurrentAllocationReplaysWithoutDoubleReservation() throws Exception {
        sql.update("INSERT INTO wms_outbound_order (id, order_no, warehouse_id, status) VALUES (301,'SO-301',301,'APPROVED')");
        sql.update("INSERT INTO wms_outbound_order_line (id, order_id, sku_id, location_id, lot_no, ordered_qty)"
                + " VALUES (301,301,11,10,'L',3)");
        sql.update("INSERT INTO wms_inventory (id, warehouse_id, location_id, sku_id, lot_no, inventory_status, on_hand_qty, available_qty)"
                + " VALUES (301,301,10,11,'L','AVAILABLE',10,10)");
        List<OutboundOrderDto> receipts = compete(8, i -> outbound.allocate(new AllocateOutboundCommand(301L, "same-request")));
        assertThat(receipts).extracting(OutboundOrderDto::getStatus).containsOnly("ALLOCATED");
        assertThat(sql.queryForObject("SELECT reserved_qty FROM wms_inventory WHERE id=301", Integer.class)).isEqualTo(3);
        assertThatThrownBy(() -> outbound.allocate(new AllocateOutboundCommand(301L, "another-key")))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void competingOrdersCannotOverReserveTheSameInventory() throws Exception {
        sql.update("INSERT INTO wms_outbound_order (id, order_no, warehouse_id, status)"
                + " VALUES (311,'SO-311',310,'APPROVED'),(312,'SO-312',310,'APPROVED')");
        sql.update("INSERT INTO wms_outbound_order_line (id, order_id, sku_id, location_id, lot_no, ordered_qty)"
                + " VALUES (311,311,11,10,'L',7),(312,312,11,10,'L',7)");
        sql.update("INSERT INTO wms_inventory (id, warehouse_id, location_id, sku_id, lot_no, inventory_status, on_hand_qty, available_qty)"
                + " VALUES (310,310,10,11,'L','AVAILABLE',10,10)");

        List<Boolean> allocated = compete(2, worker -> {
            try {
                outbound.allocate(new AllocateOutboundCommand(311L + worker, "race-" + worker));
                return true;
            } catch (DomainException rejected) {
                assertThat(rejected.getMessage()).contains("库存");
                return false;
            }
        });

        assertThat(allocated).containsExactlyInAnyOrder(true, false);
        assertThat(sql.queryForObject("SELECT reserved_qty FROM wms_inventory WHERE id=310", Integer.class)).isEqualTo(7);
        assertThat(sql.queryForList("SELECT status FROM wms_outbound_order WHERE warehouse_id=310", String.class))
                .containsExactlyInAnyOrder("APPROVED", "ALLOCATED");
        assertThat(sql.queryForObject("SELECT COUNT(*) FROM wms_allocation_receipt WHERE order_id IN (311,312)", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void httpReturnsBackPressureAndRejectsOversizedClaims() throws Exception {
        warehouse(203, 0, 1, 1);
        http.perform(post("/api/wes/warehouses/203/tasks").contentType("application/json")
                .content("{\"taskKey\":\"t1\",\"priority\":5}"))
                .andExpect(status().isTooManyRequests());
        http.perform(post("/api/wes/warehouses/203/claims").contentType("application/json")
                .content("{\"deviceCode\":\"dock\",\"claimKey\":\"c2\",\"limit\":101}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void putawayCarriesSkuAndKeepsConcurrentSpaceAndReceiptReservations() throws Exception {
        warehouse(401, 20, 8, 8);
        device(401, "dock", 8);
        location(401, 1, 5, "SAME_BATCH");
        location(401, 2, 20, "ANY");
        http.perform(post("/api/wms/warehouses/401/receipt-lines").contentType("application/json")
                .content("{\"receiptKey\":\"R1\",\"skuId\":11,\"lotNo\":\"b1\",\"quantity\":8}"))
                .andExpect(status().isOk());
        dispatch.registerReceipt(401, "R1", 11, "B1", 8);

        List<DispatchTaskDto> winners = compete(2, i -> {
            try {
                return dispatch.submitStockWork(401, "race-" + i, 10, putaway("R1", 11, "B1", 1, 4));
            } catch (DispatchException rejected) {
                assertThat(rejected.getMessage()).contains("容量");
                return null;
            }
        });
        assertThat(winners.stream().filter(java.util.Objects::nonNull)).hasSize(1);
        DispatchTaskDto winner = winners.stream().filter(java.util.Objects::nonNull).findFirst().get();
        assertThat(dispatch.submitStockWork(401, winner.taskKey, 10, putaway("R1", 11, "B1", 1, 4)).id).isEqualTo(winner.id);
        assertThatThrownBy(() -> dispatch.submitStockWork(401, winner.taskKey, 10, putaway("R1", 11, "B1", 1, 3)))
                .isInstanceOf(DispatchException.class);
        assertThatThrownBy(() -> dispatch.submitStockWork(401, "excess", 10, putaway("R1", 11, "B1", 2, 5)))
                .hasMessageContaining("实收量");

        http.perform(post("/api/wes/warehouses/401/claims").contentType("application/json")
                .content("{\"deviceCode\":\"dock\",\"claimKey\":\"claimed\",\"limit\":1}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].work.skuId").value(11))
                .andExpect(jsonPath("$.data[0].work.lotNo").value("B1"))
                .andExpect(jsonPath("$.data[0].work.destinationId").value(1))
                .andExpect(jsonPath("$.data[0].work.quantity").value(4));
        assertThatThrownBy(() -> dispatch.report(401, winner.id, "dock", "claimed", "short", SUCCEEDED, false, null, 3))
                .hasMessageContaining("全量");
        assertThat(compete(6, i -> dispatch.report(401, winner.id, "dock", "claimed", "done", SUCCEEDED, false, null, 4)))
                .extracting(task -> task.state).containsOnly("SUCCEEDED");
        assertThatThrownBy(() -> dispatch.report(401, winner.id, "dock", "claimed", "done", SUCCEEDED, false, null, 3))
                .hasMessageContaining("回执键");
        assertThat(sql.queryForObject("SELECT on_hand_qty FROM wms_inventory WHERE warehouse_id=401 AND location_id=1", Integer.class))
                .isEqualTo(4);
        assertThat(sql.queryForObject("SELECT COUNT(*) FROM wms_inventory_transaction WHERE work_task_id=?", Integer.class, winner.id))
                .isEqualTo(1);
        http.perform(get("/api/wms/warehouses/401/stock-work/" + winner.id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.task.state").value("SUCCEEDED"))
                .andExpect(jsonPath("$.data.ledger[0].action").value("PUTAWAY"))
                .andExpect(jsonPath("$.data.ledger[0].deltaOnHand").value(4))
                .andExpect(jsonPath("$.data.ledger[0].deltaAvailable").value(4));
        http.perform(get("/api/wms/warehouses/402/stock-work/" + winner.id)).andExpect(status().isNotFound());
        assertThatThrownBy(() -> dispatch.submitStockWork(401, "excess-after", 10, putaway("R1", 11, "B1", 2, 5)))
                .hasMessageContaining("实收量");
    }

    @Test
    void pendingTasksParticipateInMixRulesAndCancellationReleasesCapacity() {
        warehouse(402, 20, 8, 8);
        location(402, 1, 10, "SAME_BATCH");
        location(402, 2, 10, "SAME_SKU");
        dispatch.registerReceipt(402, "R1", 11, "B1", 8);
        dispatch.registerReceipt(402, "R2", 11, "B2", 8);
        dispatch.registerReceipt(402, "R3", 12, "B1", 8);
        DispatchTaskDto first = dispatch.submitStockWork(402, "first", 10, putaway("R1", 11, "B1", 1, 8));
        assertThatThrownBy(() -> dispatch.submitStockWork(402, "mixed-batch", 10, putaway("R2", 11, "B2", 1, 1)))
                .hasMessageContaining("混放");
        assertThatThrownBy(() -> dispatch.freezeLocation(402, 1, "CT-1", "循环盘点")).hasMessageContaining("未确认");
        assertThat(dispatch.cancelStockWork(402, first.id).state).isEqualTo("CANCELLED");
        assertThat(dispatch.cancelStockWork(402, first.id).state).isEqualTo("CANCELLED");
        dispatch.submitStockWork(402, "other-batch", 10, putaway("R2", 11, "B2", 1, 5));
        dispatch.submitStockWork(402, "same-receipt", 10, putaway("R1", 11, "B1", 2, 5));
        dispatch.submitStockWork(402, "same-sku", 10, putaway("R2", 11, "B2", 2, 2));
        assertThatThrownBy(() -> dispatch.submitStockWork(402, "mixed-sku", 10, putaway("R3", 12, "B1", 2, 1)))
                .hasMessageContaining("混放");
    }

    @Test
    void transferSettlesBothSidesAndKeepsReservationsWhileResultIsUnknown() {
        warehouse(403, 20, 8, 8);
        device(403, "agv", 8);
        location(403, 1, 20, "ANY");
        location(403, 2, 10, "SAME_BATCH");
        sql.update("INSERT INTO wms_inventory (id,warehouse_id,location_id,sku_id,lot_no,inventory_status,on_hand_qty,available_qty)"
                + " VALUES (4031,403,1,11,'B1','AVAILABLE',10,10)");

        StockWork move = new StockWork(StockWork.Type.TRANSFER, null, 11, "B1", 1L, 2, 6);
        DispatchTaskDto cancelled = dispatch.submitStockWork(403, "cancel", 10, move);
        assertThat(reserved(4031)).isEqualTo(6);
        dispatch.cancelStockWork(403, cancelled.id);
        assertThat(reserved(4031)).isZero();

        DispatchTaskDto uncertain = dispatch.submitStockWork(403, "uncertain", 10, move);
        dispatch.claim(403, "agv", "claim", 1);
        assertThatThrownBy(() -> dispatch.cancelStockWork(403, uncertain.id)).hasMessageContaining("未下发");
        dispatch.report(403, uncertain.id, "agv", "claim", "timeout", UNKNOWN, false, "回执超时", null);
        assertThat(reserved(4031)).isEqualTo(6);
        assertThatThrownBy(() -> dispatch.freezeLocation(403, 2, "CT-2", "盘点")).hasMessageContaining("未确认");
        assertThatThrownBy(() -> dispatch.report(403, uncertain.id, "agv", "claim", "failure", FAILED, true, "未移动", 1))
                .hasMessageContaining("零作业");
        dispatch.report(403, uncertain.id, "agv", "claim", "failure", FAILED, true, "现场确认未移动", 0);
        assertThat(reserved(4031)).isZero();

        DispatchTaskDto completed = dispatch.submitStockWork(403, "transfer", 10, move);
        dispatch.claim(403, "agv", "claim2", 1);
        dispatch.report(403, completed.id, "agv", "claim2", "done", SUCCEEDED, false, null, 6);
        dispatch.report(403, completed.id, "agv", "claim2", "done", SUCCEEDED, false, null, 6);
        assertThat(reserved(4031)).isZero();
        assertThat(sql.queryForList("SELECT on_hand_qty FROM wms_inventory WHERE warehouse_id=403 ORDER BY location_id", Integer.class))
                .containsExactly(4, 6);
        assertThat(sql.queryForList("SELECT biz_type FROM wms_inventory_transaction WHERE work_task_id=? ORDER BY id",
                String.class, completed.id)).containsExactly("MOVE_RESERVE", "MOVE_OUT", "MOVE_IN");
        assertThat(sql.queryForObject("SELECT SUM(quantity) FROM wms_inventory_transaction WHERE work_task_id=?",
                Integer.class, completed.id)).isZero();
    }

    @Test
    void releasedFreezeCannotBeReplayedAndWarehouseAllocationUsesSameLocationLock() {
        warehouse(404, 20, 8, 8);
        location(404, 1, 20, "ANY");
        dispatch.registerReceipt(404, "R", 11, "B1", 2);
        sql.update("INSERT INTO wms_inventory (id,warehouse_id,location_id,sku_id,lot_no,inventory_status,on_hand_qty,available_qty)"
                + " VALUES (4041,404,1,11,'B1','AVAILABLE',10,10)");
        sql.update("INSERT INTO wms_outbound_order (id,order_no,warehouse_id,status) VALUES (4041,'SO-404',404,'APPROVED')");
        sql.update("INSERT INTO wms_outbound_order_line (id,order_id,sku_id,location_id,lot_no,ordered_qty)"
                + " VALUES (4041,4041,11,1,'B1',3)");

        assertThat(dispatch.freezeLocation(404, 1, "CT-1", "盘点")).isTrue();
        assertThatThrownBy(() -> dispatch.submitStockWork(404, "blocked", 10, putaway("R", 11, "B1", 1, 1)))
                .hasMessageContaining("已冻结");
        assertThatThrownBy(() -> outbound.allocate(new AllocateOutboundCommand(4041L, "ok")))
                .hasMessageContaining("已冻结");
        dispatch.unfreezeLocation(404, 1, "CT-1");
        assertThat(dispatch.freezeLocation(404, 1, "CT-1", "盘点")).isFalse();
        assertThat(dispatch.freezeLocation(404, 1, "CT-2", "复盘")).isTrue();
        dispatch.unfreezeLocation(404, 1, "CT-1");
        assertThatThrownBy(() -> outbound.allocate(new AllocateOutboundCommand(4041L, "ok")))
                .hasMessageContaining("已冻结");
        dispatch.unfreezeLocation(404, 1, "CT-2");
        outbound.allocate(new AllocateOutboundCommand(4041L, "ok"));
        outbound.allocate(new AllocateOutboundCommand(4041L, "ok"));
        assertThat(reserved(4041)).isEqualTo(3);
        assertThat(sql.queryForList("SELECT delta_reserved_qty FROM wms_inventory_transaction WHERE outbound_line_id=4041",
                Integer.class)).containsExactly(3);
        assertThatThrownBy(() -> dispatch.freezeLocation(404, 1, "CT-3", "重复盘点"))
                .hasMessageContaining("预占");
    }

    @Test
    void inventoryAndReceiptRollbackIfTransferLedgerCannotBeCommitted() {
        warehouse(405, 20, 8, 8);
        device(405, "agv", 8);
        location(405, 1, 20, "ANY");
        location(405, 2, 20, "ANY");
        sql.update("INSERT INTO wms_inventory (id,warehouse_id,location_id,sku_id,lot_no,inventory_status,on_hand_qty,available_qty)"
                + " VALUES (4051,405,1,11,'B1','AVAILABLE',10,10)");
        StockWork move = new StockWork(StockWork.Type.TRANSFER, null, 11, "B1", 1L, 2, 6);
        DispatchTaskDto task = dispatch.submitStockWork(405, "rollback", 10, move);
        dispatch.claim(405, "agv", "claim", 1);
        sql.update("INSERT INTO wms_inventory_transaction (warehouse_id,location_id,sku_id,lot_no,biz_type,biz_no,quantity,"
                + "before_on_hand_qty,after_on_hand_qty,work_task_id) VALUES (405,2,11,'B1','MOVE_IN','fixture',0,0,0,?)", task.id);
        assertThatThrownBy(() -> dispatch.report(405, task.id, "agv", "claim", "done", SUCCEEDED, false, null, 6))
                .isInstanceOf(org.springframework.dao.DuplicateKeyException.class);
        assertThat(dispatch.stockWorkDetail(405, task.id).task.state).isEqualTo("IN_FLIGHT");
        assertThat(reserved(4051)).isEqualTo(6);
        assertThat(sql.queryForList("SELECT on_hand_qty FROM wms_inventory WHERE warehouse_id=405", Integer.class))
                .containsExactly(10);
        assertThat(sql.queryForObject("SELECT COUNT(*) FROM wes_dispatch_event WHERE task_id=?", Integer.class, task.id)).isZero();
    }

    @Test
    void readyTransferCancelAndClaimAreMutuallyExclusive() throws Exception {
        warehouse(99001, 20, 8, 8);
        device(99001, "agent-test", 8);
        location(99001, 1, 20, "ANY");
        location(99001, 2, 20, "ANY");
        sql.update("INSERT INTO wms_inventory (id,warehouse_id,location_id,sku_id,lot_no,inventory_status,on_hand_qty,available_qty)"
                + " VALUES (99001,99001,1,11,'B1','AVAILABLE',10,10)");
        StockWork work = new StockWork(StockWork.Type.TRANSFER, null, 11, "B1", 1L, 2, 6);
        DispatchTaskDto task = dispatch.submitStockWork(99001, "cancel-race", 10, work);
        List<Boolean> results = compete(2, worker -> {
            if (worker == 0) {
                try {
                    return "CANCELLED".equals(dispatch.cancelStockWork(99001, task.id).state);
                } catch (DispatchException e) {
                    assertThat(e.getMessage()).contains("未下发");
                    return false;
                }
            }
            return !dispatch.claim(99001, "agent-test", "race-claim", 1).isEmpty();
        });
        assertThat(results).containsExactlyInAnyOrder(true, false);
        String state = dispatch.stockWorkDetail(99001, task.id).task.state;
        assertThat(state).isIn("CANCELLED", "IN_FLIGHT");
        assertThat(reserved(99001)).isEqualTo("CANCELLED".equals(state) ? 0 : 6);
        assertThat(sql.queryForObject("SELECT COUNT(*) FROM wms_inventory_transaction WHERE work_task_id=? AND biz_type='MOVE_RELEASE'",
                Integer.class, task.id)).isEqualTo("CANCELLED".equals(state) ? 1 : 0);
    }

    @Test
    void concurrentReadyCancellationReleasesExactlyOnceAndScopesWarehouse() throws Exception {
        warehouse(99002, 20, 8, 8);
        warehouse(99003, 20, 8, 8);
        location(99002, 1, 20, "ANY");
        location(99002, 2, 20, "ANY");
        sql.update("INSERT INTO wms_inventory (id,warehouse_id,location_id,sku_id,lot_no,inventory_status,on_hand_qty,available_qty)"
                + " VALUES (99002,99002,1,11,'B1','AVAILABLE',10,10)");
        DispatchTaskDto task = dispatch.submitStockWork(99002, "cancel-repeat", 10,
                new StockWork(StockWork.Type.TRANSFER, null, 11, "B1", 1L, 2, 6));
        assertThatThrownBy(() -> dispatch.cancelStockWork(99003, task.id)).isInstanceOf(DispatchException.class);
        assertThat(reserved(99002)).isEqualTo(6);
        assertThat(compete(6, worker -> dispatch.cancelStockWork(99002, task.id)))
                .extracting(t -> t.state).containsOnly("CANCELLED");
        assertThat(reserved(99002)).isZero();
        assertThat(sql.queryForObject("SELECT COUNT(*) FROM wms_inventory_transaction WHERE work_task_id=? AND biz_type='MOVE_RELEASE'",
                Integer.class, task.id)).isEqualTo(1);
        assertThat(sql.queryForObject("SELECT on_hand_qty FROM wms_inventory WHERE id=99002", Integer.class)).isEqualTo(10);
    }

    private void location(long warehouse, long location, int maxUnits, String policy) {
        sql.update("INSERT INTO wms_work_location (warehouse_id,location_id,max_units,mix_policy) VALUES (?,?,?,?)",
                warehouse, location, maxUnits, policy);
    }

    private int reserved(long inventoryId) {
        return sql.queryForObject("SELECT reserved_qty FROM wms_inventory WHERE id=?", Integer.class, inventoryId);
    }

    private StockWork putaway(String receipt, long sku, String lot, long destination, int quantity) {
        return new StockWork(StockWork.Type.PUTAWAY, receipt, sku, lot, null, destination, quantity);
    }

    private void warehouse(long id, int ready, int active, int batch) {
        sql.update("INSERT INTO wes_dispatch_capacity VALUES (?,?,?,?)", id, ready, active, batch);
    }

    private void device(long warehouse, String code, int active) {
        sql.update("INSERT INTO wes_dispatch_device VALUES (?,?,?)", warehouse, code, active);
    }

    private <T> List<T> compete(int size, IntFunction<T> action) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(size);
        CountDownLatch ready = new CountDownLatch(size);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<T>> futures = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                final int worker = i;
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return action.apply(worker);
                }));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            List<T> results = new ArrayList<>();
            for (Future<T> future : futures) {
                results.add(future.get(20, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            executor.shutdownNow();
        }
    }
}
