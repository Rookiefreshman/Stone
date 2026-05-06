package com.stone.wms.domain;

import java.time.Instant;
import java.util.UUID;

public record StockMovement(
        UUID id,
        MovementType type,
        String sku,
        String location,
        int quantity,
        String referenceNo,
        Instant occurredAt
) {
    public static StockMovement create(MovementType type, String sku, String location, int quantity, String referenceNo) {
        return new StockMovement(UUID.randomUUID(), type, sku, location, quantity, referenceNo, Instant.now());
    }

    public String toJson() {
        return "{\"id\":\"%s\",\"type\":\"%s\",\"sku\":\"%s\",\"location\":\"%s\",\"quantity\":%d,\"referenceNo\":\"%s\",\"occurredAt\":\"%s\"}"
                .formatted(id, type, sku, location, quantity, referenceNo, occurredAt);
    }
}
