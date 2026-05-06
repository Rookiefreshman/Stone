package com.stone.wms.domain;

import java.time.Instant;
import java.util.Objects;

public class InventoryItem {
    private final String sku;
    private final String location;
    private int quantity;
    private Instant updatedAt;

    public InventoryItem(String sku, String location, int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("quantity must be greater than or equal to zero");
        }
        this.sku = normalize(sku, "sku");
        this.location = normalize(location, "location");
        this.quantity = quantity;
        this.updatedAt = Instant.now();
    }

    public String getSku() {
        return sku;
    }

    public String getLocation() {
        return location;
    }

    public int getQuantity() {
        return quantity;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void increase(int delta) {
        requirePositive(delta);
        quantity += delta;
        updatedAt = Instant.now();
    }

    public void decrease(int delta) {
        requirePositive(delta);
        if (quantity < delta) {
            throw new IllegalArgumentException("not enough quantity at location " + location);
        }
        quantity -= delta;
        updatedAt = Instant.now();
    }

    private static void requirePositive(int value) {
        if (value <= 0) {
            throw new IllegalArgumentException("quantity must be greater than zero");
        }
    }

    private static String normalize(String value, String field) {
        Objects.requireNonNull(value, field + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return normalized.toUpperCase();
    }
}
