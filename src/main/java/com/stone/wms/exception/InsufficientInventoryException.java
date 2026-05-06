package com.stone.wms.exception;

public class InsufficientInventoryException extends RuntimeException {
    public InsufficientInventoryException(String sku, int requested, int available) {
        super("Insufficient inventory for SKU %s: requested=%d, available=%d".formatted(sku, requested, available));
    }
}
