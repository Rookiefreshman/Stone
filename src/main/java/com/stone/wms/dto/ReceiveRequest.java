package com.stone.wms.dto;

public record ReceiveRequest(String sku, String location, int quantity, String referenceNo) {
    public ReceiveRequest {
        requireText(sku, "sku");
        requireText(location, "location");
        requireText(referenceNo, "referenceNo");
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be greater than zero");
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
