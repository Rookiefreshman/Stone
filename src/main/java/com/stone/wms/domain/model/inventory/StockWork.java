package com.stone.wms.domain.model.inventory;

import java.util.Objects;

public final class StockWork {
    public enum Type { PUTAWAY, TRANSFER }

    public final Type type;
    public final String receiptKey;
    public final long skuId;
    public final String lotNo;
    public final Long sourceId;
    public final long destinationId;
    public final int quantity;

    public StockWork(Type type, String receiptKey, long skuId, String lotNo, Long sourceId, long destinationId, int quantity) {
        this.type = type;
        this.receiptKey = receiptKey;
        this.skuId = skuId;
        this.lotNo = lotNo;
        this.sourceId = sourceId;
        this.destinationId = destinationId;
        this.quantity = quantity;
    }

    public boolean sameAs(StockWork other) {
        return type == other.type && skuId == other.skuId && lotNo.equals(other.lotNo)
                && Objects.equals(receiptKey, other.receiptKey) && Objects.equals(sourceId, other.sourceId)
                && destinationId == other.destinationId && quantity == other.quantity;
    }
}
