package com.stone.warehouse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=${STONE_TEST_DB_URL:jdbc:h2:mem:stone-tests;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1}",
        "spring.datasource.username=${STONE_TEST_DB_USER:sa}",
        "spring.datasource.password=${STONE_TEST_DB_PASSWORD:}"
})
@AutoConfigureMockMvc
class WarehouseFlowTest {
    @Autowired MockMvc http;
    @Autowired ObjectMapper json;

    private String warehouse;
    private String sku;
    private String storage;
    private String picking;
    private String quarantine;
    private String device;

    @BeforeEach
    void createWarehouse() throws Exception {
        warehouse = id(postJson("/api/warehouses", body("code", "W-" + java.util.UUID.randomUUID(), "name", "验收仓")));
        sku = master("skus", "SKU", "样品", null, null);
        storage = master("locations", "A01", "存储位", "locationType", "STORAGE");
        picking = master("locations", "P01", "拣选位", "locationType", "PICK_FACE");
        quarantine = master("locations", "Q01", "隔离位", "locationType", "QUARANTINE");
        device = master("devices", "SIM1", "模拟站", "deviceMode", "SIMULATOR");
    }

    @Test
    void receiptReplenishCountShipAndReturnReconcileStock() throws Exception {
        receipt("PO-1", "LOT-A", null, 20);
        String replenish = create("REPLENISH", "RP-1", null, line("LOT-A", storage, picking, 8));
        action(replenish, "APPROVE");
        action(replenish, "RELEASE");
        assertStock(storage, "LOT-A", 20, 8);
        finish(task(replenish), null);
        assertStock(storage, "LOT-A", 12, 0);
        assertStock(picking, "LOT-A", 8, 0);

        String count = create("COUNT", "CT-1", null, line("LOT-A", picking, null, null));
        action(count, "APPROVE");
        action(count, "RELEASE");
        String move = create("TRANSFER", "MOVE-1", null, line("LOT-A", storage, picking, 1));
        action(move, "APPROVE");
        actionResult(move, "RELEASE").andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("COUNT_LOCKED"));
        String manual = master("devices", "MAN1", "人工站", "deviceMode", "MANUAL");
        String command = dispatch(task(count), manual);
        event(command, "start", "STARTED", null, null);
        event(command, "finish", "SUCCEEDED", 7, null);
        assertStock(picking, "LOT-A", 8, 0);
        action(count, "ACCEPT_COUNT");
        assertStock(picking, "LOT-A", 7, 0);
        action(move, "RELEASE");
        finish(task(move), null);
        assertStock(storage, "LOT-A", 11, 0);
        assertStock(picking, "LOT-A", 8, 0);

        String outbound = create("OUTBOUND", "SO-1", null, line("LOT-A", picking, null, 4));
        action(outbound, "APPROVE");
        action(outbound, "RELEASE");
        assertStock(picking, "LOT-A", 8, 4);
        finish(task(outbound), null);
        assertThat(state(outbound)).isEqualTo("PICKED");
        action(outbound, "PACK");
        action(outbound, "SHIP");
        assertStock(picking, "LOT-A", 4, 0);

        String returns = create("RETURN", "RMA-1", outbound, line("LOT-A", null, quarantine, 3));
        request(scoped("documents"), doc("RETURN", "RMA-2", outbound, line("LOT-A", null, quarantine, 2)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("RETURN_EXCEEDS_SHIPMENT"));
        action(returns, "APPROVE");
        action(returns, "RECEIVE");
        finish(task(returns), null);
        assertStock(quarantine, "LOT-A", 3, 0);
        String blocked = create("OUTBOUND", "SO-Q", null, line("LOT-A", null, null, 16));
        action(blocked, "APPROVE");
        actionResult(blocked, "RELEASE").andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"));
        assertThat(detail(blocked).get("tasks").isEmpty()).isTrue();
        assertThat(getJson(scoped("ledger")).findValuesAsText("reason"))
                .contains("PUTAWAY", "RESERVE", "MOVE_OUT", "MOVE_IN", "SHIP", "COUNT_ADJUST");
    }

    @Test
    void handlesDuplicateOutOfOrderAndFailedDeviceEvents() throws Exception {
        Map<String, Object> payload = doc("INBOUND", "PO-RETRY", null, line("B1", null, storage, 2));
        String inbound = id(postJson(scoped("documents"), payload));
        assertThat(id(postJson(scoped("documents"), payload))).isEqualTo(inbound);
        request(scoped("documents"), doc("INBOUND", "PO-RETRY", null, line("B1", null, storage, 9)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("REQUEST_CONFLICT"));

        actionResult(inbound, "RECEIVE").andExpect(status().isConflict());
        action(inbound, "APPROVE");
        action(inbound, "RECEIVE");
        String task = task(inbound);
        String first = dispatch(task, device);
        eventResult(first, "early", "SUCCEEDED", null, null).andExpect(status().isConflict());
        event(first, "start", "STARTED", null, null);
        event(first, "break", "FAILED", null, "模拟堵塞");
        assertThat(detail(inbound).at("/tasks/0/state").asText()).isEqualTo("EXCEPTION");

        String secondReceipt = create("INBOUND", "PO-RETRY-2", null, line("B1", null, storage, 1));
        action(secondReceipt, "APPROVE");
        action(secondReceipt, "RECEIVE");
        request("/api/tasks/" + task(secondReceipt) + "/dispatch", body("deviceId", device))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("DEVICE_BUSY"));
        reconcile(task, false);
        String second = dispatch(task, device);
        eventResult(first, "late", "SUCCEEDED", null, null).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STALE_COMMAND"));
        event(second, "start", "STARTED", null, null);
        event(second, "finish", "SUCCEEDED", null, null);
        event(second, "finish", "SUCCEEDED", null, null);
        eventResult(second, "finish", "FAILED", null, "不同内容")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("EVENT_CONFLICT"));
        assertStock(storage, "B1", 2, 0);
        assertThat(getJson("/api/tasks/" + task).get("commands")).hasSize(2);

        String failedCommand = dispatch(task(secondReceipt), device);
        event(failedCommand, "failure", "FAILED", null, "回执超时");
        reconcile(task(secondReceipt), true);
        assertStock(storage, "B1", 3, 0);
        assertThat(state(secondReceipt)).isEqualTo("COMPLETED");
        assertThat(getJson("/api/commands/" + failedCommand).at("/command/state").asText()).isEqualTo("FAILED");
    }

    @Test
    void splitsByFefoAndProtectsReservationsUnderConcurrentRelease() throws Exception {
        receipt("FIRST", "LATER", "2099-12-01", 8);
        receipt("SECOND", "EARLIER", "2099-10-01", 4);
        receipt("THIRD", "EXPIRED", "2000-01-01", 50);
        String a = create("OUTBOUND", "OUT-A", null, line(null, null, null, 7));
        String b = create("OUTBOUND", "OUT-B", null, line(null, null, null, 7));
        action(a, "APPROVE");
        action(b, "APPROVE");
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Integer> statusA = pool.submit(() -> {
                start.await();
                return actionResult(a, "RELEASE").andReturn().getResponse().getStatus();
            });
            Future<Integer> statusB = pool.submit(() -> {
                start.await();
                return actionResult(b, "RELEASE").andReturn().getResponse().getStatus();
            });
            start.countDown();
            int aCode = statusA.get(10, TimeUnit.SECONDS);
            int bCode = statusB.get(10, TimeUnit.SECONDS);
            assertThat(new int[] {aCode, bCode}).containsExactlyInAnyOrder(200, 409);
            String winner = aCode == 200 ? a : b;
            String loser = aCode == 409 ? a : b;
            assertThat(detail(winner).get("tasks")).hasSize(2);
            assertThat(detail(loser).get("tasks")).isEmpty();
            assertStock(storage, "EARLIER", 4, 4);
            assertStock(storage, "LATER", 8, 3);
            assertStock(storage, "EXPIRED", 50, 0);
            action(winner, "CANCEL");
            action(loser, "RELEASE");
            for (JsonNode task : detail(loser).get("tasks")) {
                finish(id(task), null);
            }
            action(loser, "PACK");
            action(loser, "UNPICK_CANCEL");
            assertStock(storage, "EARLIER", 4, 0);
            assertStock(storage, "LATER", 8, 0);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void rejectsMalformedLinesCrossWarehouseLocationsAndDuplicateCountScopes() throws Exception {
        String other = id(postJson("/api/warehouses", body("code", "O-" + java.util.UUID.randomUUID(), "name", "其他仓")));
        String foreign = id(postJson("/api/warehouses/" + other + "/locations",
                body("code", "B-01", "name", "外仓位", "locationType", "STORAGE")));
        request(scoped("documents"), doc("INBOUND", "BAD", null, line("B1", null, foreign, 1)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("WRONG_WAREHOUSE"));
        request(scoped("documents"), doc("INBOUND", "NULL-LINE", null, (Object) null))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        receipt("COUNT-BATCH", "B1", null, 3);
        String count = create("COUNT", "CT-DUP", null, line("B1", storage, null, null), line("B1", storage, null, null));
        action(count, "APPROVE");
        actionResult(count, "RELEASE").andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COUNT_SCOPE_BUSY"));
        assertThat(detail(count).get("tasks")).isEmpty();
        assertThat(state(count)).isEqualTo("APPROVED");
        http.perform(get(scoped("inventory")).param("limit", "201")).andExpect(status().isBadRequest());
        http.perform(get("/")).andExpect(status().isOk());
    }

    @Test
    void releasesWaveAtomicallyAndPrioritizesItsQueue() throws Exception {
        receipt("WAVE-STOCK", "B1", null, 10);
        String regular = create("OUTBOUND", "REGULAR", null, line(null, null, null, 2));
        action(regular, "APPROVE");
        action(regular, "RELEASE");
        String first = create("OUTBOUND", "WAVE-A", null, line(null, null, null, 5));
        String second = create("OUTBOUND", "WAVE-B", null, line(null, null, null, 4));
        action(first, "APPROVE");
        action(second, "APPROVE");
        String wave = id(postJson(scoped("waves"), body("code", "WAVE-MORNING", "priority", 60,
                "documentIds", new String[] {first, second})));
        actionResult(first, "RELEASE").andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("WAVE_ASSIGNED"));
        request("/api/waves/" + wave + "/actions/RELEASE", body("reason", "早班释放"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"));
        assertThat(state(first)).isEqualTo("APPROVED");
        assertThat(detail(first).get("tasks")).isEmpty();
        assertStock(storage, "B1", 10, 2);
        receipt("WAVE-STOCK-EXTRA", "B1", null, 1);
        postJson("/api/waves/" + wave + "/actions/RELEASE", body("reason", "补足库存后释放"));
        assertThat(getJson("/api/waves/" + wave).at("/wave/state").asText()).isEqualTo("RELEASED");
        JsonNode queue = getJson(scoped("tasks"));
        assertThat(queue.get(2).path("documentId").asText()).isEqualTo(regular);
        assertStock(storage, "B1", 11, 11);

        String waiting = create("OUTBOUND", "WAITING", null, line(null, null, null, 1));
        action(waiting, "APPROVE");
        String cancelled = id(postJson(scoped("waves"), body("code", "WAVE-CANCEL", "priority", 1,
                "documentIds", new String[] {waiting})));
        postJson("/api/waves/" + cancelled + "/actions/CANCEL", body("reason", "重排"));
        postJson(scoped("waves"), body("code", "WAVE-REPLAN", "priority", 1, "documentIds", new String[] {waiting}));
    }

    private String master(String path, String code, String name, String type, String value) throws Exception {
        Map<String, Object> data = body("code", code, "name", name);
        if (type != null) data.put(type, value);
        return id(postJson(scoped(path), data));
    }

    private void receipt(String ref, String batch, String expiry, int quantity) throws Exception {
        Map<String, Object> item = line(batch, null, storage, quantity);
        if (expiry != null) item.put("expiresOn", expiry);
        String id = create("INBOUND", ref, null, item);
        action(id, "APPROVE");
        action(id, "RECEIVE");
        finish(task(id), null);
    }

    private Map<String, Object> line(String batch, String source, String destination, Integer quantity) {
        return body("skuId", sku, "batchCode", batch, "sourceId", source, "destinationId", destination, "quantity", quantity);
    }

    private Map<String, Object> doc(String kind, String ref, String source, Object... lines) {
        return body("kind", kind, "externalRef", ref, "sourceDocumentId", source, "lines", lines);
    }

    private String create(String kind, String ref, String source, Object... lines) throws Exception {
        return id(postJson(scoped("documents"), doc(kind, ref, source, lines)));
    }

    private void finish(String task, Integer quantity) throws Exception {
        String command = dispatch(task, device);
        event(command, "start", "STARTED", null, null);
        event(command, "finish", "SUCCEEDED", quantity, null);
    }

    private String dispatch(String task, String deviceId) throws Exception {
        return id(postJson("/api/tasks/" + task + "/dispatch", body("deviceId", deviceId)));
    }

    private void event(String command, String key, String type, Integer quantity, String detail) throws Exception {
        eventResult(command, key, type, quantity, detail).andExpect(status().isOk());
    }

    private ResultActions eventResult(String command, String key, String type, Integer quantity, String detail) throws Exception {
        return request("/api/commands/" + command + "/events",
                body("eventKey", key, "eventType", type, "observedQuantity", quantity, "detail", detail));
    }

    private void reconcile(String task, boolean completed) throws Exception {
        postJson("/api/tasks/" + task + "/reconcile", body("completed", completed, "reason", "已现场核对货物与设备"));
    }

    private void assertStock(String location, String batch, int onHand, int reserved) throws Exception {
        for (JsonNode balance : getJson(scoped("inventory"))) {
            if (location.equals(balance.get("locationId").asText()) && batch.equals(balance.get("batchCode").asText())) {
                assertThat(balance.get("onHand").asInt()).isEqualTo(onHand);
                assertThat(balance.get("reserved").asInt()).isEqualTo(reserved);
                return;
            }
        }
        throw new AssertionError("库存余额不存在 " + batch);
    }

    private String scoped(String path) { return "/api/warehouses/" + warehouse + "/" + path; }
    private JsonNode detail(String id) throws Exception { return getJson("/api/documents/" + id); }
    private String task(String id) throws Exception { return detail(id).at("/tasks/0/id").asText(); }
    private String state(String id) throws Exception { return detail(id).at("/document/state").asText(); }
    private String id(JsonNode node) { return node.get("id").asText(); }

    private void action(String doc, String action) throws Exception {
        actionResult(doc, action).andExpect(status().isOk());
    }

    private ResultActions actionResult(String doc, String action) throws Exception {
        return request("/api/documents/" + doc + "/actions/" + action, body("reason", "验收记录"));
    }

    private ResultActions request(String path, Object payload) throws Exception {
        return http.perform(post(path).contentType("application/json").content(json.writeValueAsBytes(payload)));
    }

    private JsonNode getJson(String path) throws Exception {
        return json.readTree(http.perform(get(path)).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray());
    }

    private JsonNode postJson(String path, Object payload) throws Exception {
        return json.readTree(request(path, payload).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray());
    }

    private Map<String, Object> body(Object... pairs) {
        Map<String, Object> data = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            data.put((String) pairs[i], pairs[i + 1]);
        }
        return data;
    }
}
