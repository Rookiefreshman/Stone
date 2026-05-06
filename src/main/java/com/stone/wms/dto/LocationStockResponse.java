package com.stone.wms.dto;

import java.time.Instant;

public record LocationStockResponse(String location, int quantity, Instant updatedAt) {
    public String toJson() {
        return "{\"location\":\"%s\",\"quantity\":%d,\"updatedAt\":\"%s\"}"
                .formatted(location, quantity, updatedAt);
    }
}
