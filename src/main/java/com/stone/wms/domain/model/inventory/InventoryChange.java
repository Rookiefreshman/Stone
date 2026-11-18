package com.stone.wms.domain.model.inventory;

public final class InventoryChange {
    private final InventoryDimension dimension;
    private final int beforeOnHand;
    private final int beforeAvailable;
    private final int beforeReserved;
    private final int beforeFrozen;

    public InventoryChange(Inventory inventory) {
        dimension = inventory.getDimension();
        beforeOnHand = inventory.getOnHandQty();
        beforeAvailable = inventory.getAvailableQty();
        beforeReserved = inventory.getReservedQty();
        beforeFrozen = inventory.getFrozenQty();
    }

    public Entry entry(Inventory after, String action, String reference, Long taskId, Long outboundLineId) {
        return new Entry(dimension, action, reference, taskId, outboundLineId, beforeOnHand, after.getOnHandQty(),
                after.getAvailableQty() - beforeAvailable, after.getReservedQty() - beforeReserved,
                after.getFrozenQty() - beforeFrozen);
    }

    public static final class Entry {
        public final InventoryDimension dimension;
        public final String action;
        public final String reference;
        public final Long taskId;
        public final Long outboundLineId;
        public final int beforeOnHand;
        public final int afterOnHand;
        public final int deltaOnHand;
        public final int deltaAvailable;
        public final int deltaReserved;
        public final int deltaFrozen;

        private Entry(InventoryDimension dimension, String action, String reference, Long taskId, Long outboundLineId,
                      int beforeOnHand, int afterOnHand, int deltaAvailable, int deltaReserved, int deltaFrozen) {
            this.dimension = dimension;
            this.action = action;
            this.reference = reference;
            this.taskId = taskId;
            this.outboundLineId = outboundLineId;
            this.beforeOnHand = beforeOnHand;
            this.afterOnHand = afterOnHand;
            this.deltaOnHand = afterOnHand - beforeOnHand;
            this.deltaAvailable = deltaAvailable;
            this.deltaReserved = deltaReserved;
            this.deltaFrozen = deltaFrozen;
        }
    }
}
