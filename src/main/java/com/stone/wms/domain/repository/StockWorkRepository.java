package com.stone.wms.domain.repository;

import com.stone.wms.domain.model.inventory.StockWork;

import java.util.List;

public interface StockWorkRepository {
    enum MixPolicy { ANY, SAME_SKU, SAME_BATCH }

    class Location {
        public int maxUnits;
        public MixPolicy mixPolicy;
        public String freezeKey;
    }

    class Receipt {
        public long skuId;
        public String lotNo;
        public int receivedQty;
    }

    class LedgerRow {
        public long locationId;
        public String action;
        public int deltaOnHand;
        public int deltaAvailable;
        public int deltaReserved;
        public int deltaFrozen;
    }

    List<LedgerRow> ledger(long warehouse, long taskId);
    Location lockLocation(long warehouse, long location);
    long onHand(long warehouse, long location);
    long incoming(long warehouse, long location);
    boolean hasMixConflict(long warehouse, StockWork work, MixPolicy policy);
    boolean hasActiveStockOrWork(long warehouse, long location);
    String freezeReason(long warehouse, long location, String key);
    void openFreeze(long warehouse, long location, String key, String reason);
    void closeFreeze(long warehouse, long location, String key);
    Receipt receipt(long warehouse, String key);
    void insertReceipt(long warehouse, String key, long sku, String lot, int quantity);
    long committedReceipt(long warehouse, String key);
    void insertWork(long taskId, long warehouse, StockWork work);
}
