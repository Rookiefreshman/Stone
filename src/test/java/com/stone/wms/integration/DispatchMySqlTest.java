package com.stone.wms.integration;

import com.stone.wms.application.dto.AllocateOutboundCommand;
import com.stone.wms.application.dto.DispatchTaskDto;
import com.stone.wms.application.dto.OutboundOrderDto;
import com.stone.wms.application.service.DispatchApplicationService;
import com.stone.wms.application.service.OutboundAllocationApplicationService;
import com.stone.wms.domain.exception.DispatchException;
import com.stone.wms.domain.exception.DomainException;
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
