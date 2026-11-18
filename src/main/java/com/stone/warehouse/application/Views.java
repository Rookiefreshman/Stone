package com.stone.warehouse.application;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.stone.warehouse.domain.WarehouseModel.*;

public final class Views {
    private Views() {}

    public static Map<String, Object> fields(Object... entries) {
        Map<String, Object> view = new LinkedHashMap<>();
        for (int i = 0; i < entries.length; i += 2) {
            view.put((String) entries[i], entries[i + 1]);
        }
        return view;
    }

    public static Map<String, Object> master(Master m) {
        return fields("id", m.id, "warehouseId", m.warehouseId, "code", m.code, "name", m.name,
                "locationType", m.locationType, "deviceMode", m.deviceMode, "state", m.state);
    }

    public static Map<String, Object> document(Document d) {
        return fields("id", d.id, "warehouseId", d.warehouseId, "kind", d.kind, "externalRef", d.externalRef,
                "sourceDocumentId", d.sourceDocumentId, "state", d.state, "createdAt", d.createdAt);
    }

    public static Map<String, Object> line(Line l) {
        return fields("id", l.id, "lineNo", l.lineNo, "skuId", l.skuId, "lotId", l.lotId, "batchCode", l.batchCode,
                "sourceId", l.sourceId, "destinationId", l.destinationId, "quantity", l.quantity);
    }

    public static Map<String, Object> task(Task t) {
        return fields("id", t.id, "documentId", t.documentId, "lineId", t.lineId, "lotId", t.lotId, "batchCode", t.batchCode,
                "sourceId", t.sourceId, "destinationId", t.destinationId, "quantity", t.quantity,
                "observedQuantity", t.observedQuantity, "state", t.state, "attempt", t.attempt);
    }

    public static Map<String, Object> stock(Stock s) {
        return fields("locationId", s.locationId, "locationCode", s.locationCode, "locationType", s.locationType, "skuId", s.skuId,
                "skuCode", s.skuCode, "lotId", s.lotId, "batchCode", s.batchCode,
                "expiresOn", s.expiresOn, "onHand", s.onHand, "reserved", s.reserved, "available", s.available());
    }

    public static Map<String, Object> wave(Wave w) {
        return fields("id", w.id, "warehouseId", w.warehouseId, "code", w.code, "priority", w.priority,
                "state", w.state, "reason", w.reason, "createdAt", w.createdAt);
    }

    public static Map<String, Object> command(Command c) {
        return fields("id", c.id, "taskId", c.taskId, "deviceId", c.deviceId, "attempt", c.attempt,
                "state", c.state, "createdAt", c.createdAt);
    }

    public static Map<String, Object> event(CommandEvent e) {
        return fields("eventKey", e.eventKey, "eventType", e.eventType, "observedQuantity", e.observedQuantity,
                "detail", e.detail);
    }

    public static Map<String, Object> ledger(Ledger l) {
        return fields("id", l.id, "taskId", l.taskId, "locationId", l.locationId, "lotId", l.lotId,
                "batchCode", l.batchCode, "reason", l.reason, "deltaOnHand", l.deltaOnHand, "deltaReserved", l.deltaReserved,
                "createdAt", l.createdAt);
    }

    public static Map<String, Object> audit(Audit a) {
        return fields("id", a.id, "taskId", a.taskId, "action", a.action, "detail", a.detail, "createdAt", a.createdAt);
    }
}
