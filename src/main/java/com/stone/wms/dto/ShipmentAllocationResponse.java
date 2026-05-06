package com.stone.wms.dto;

public record ShipmentAllocationResponse(String sku, String location, int quantity, String referenceNo) {
    public String toJson() {
        return "{\"sku\":\"%s\",\"location\":\"%s\",\"quantity\":%d,\"referenceNo\":\"%s\"}"
                .formatted(sku, location, quantity, referenceNo);
    }
}
