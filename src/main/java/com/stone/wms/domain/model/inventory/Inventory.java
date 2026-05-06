package com.stone.wms.domain.model.inventory;

import com.stone.wms.domain.exception.DomainException;

/**
 * 库存聚合根，集中维护 WMS 最核心的数量一致性规则。
 */
public class Inventory {
    private Long inventoryId;
    private InventoryDimension dimension;
    private int onHandQty;
    private int availableQty;
    private int reservedQty;
    private int frozenQty;
    private int version;

    public Inventory(Long inventoryId, InventoryDimension dimension, int onHandQty, int availableQty,
                     int reservedQty, int frozenQty, int version) {
        this.inventoryId = inventoryId;
        this.dimension = dimension;
        this.onHandQty = onHandQty;
        this.availableQty = availableQty;
        this.reservedQty = reservedQty;
        this.frozenQty = frozenQty;
        this.version = version;
        ensureQuantityInvariant();
    }

    /**
     * 预占库存时只移动可用量到已分配量，实际出库发运时再扣减账面库存。
     */
    public void reserve(int quantity) {
        assertPositive(quantity);
        if (availableQty < quantity) {
            throw new DomainException("库存不足，无法分配");
        }
        availableQty -= quantity;
        reservedQty += quantity;
        version += 1;
        ensureQuantityInvariant();
    }

    /**
     * 取消出库或分配失败回滚时释放已分配库存。
     */
    public void release(int quantity) {
        assertPositive(quantity);
        if (reservedQty < quantity) {
            throw new DomainException("已分配库存不足，无法释放");
        }
        reservedQty -= quantity;
        availableQty += quantity;
        version += 1;
        ensureQuantityInvariant();
    }

    /**
     * 发运时扣减账面库存和已分配库存，确保库存流水能对应真实出库动作。
     */
    public void ship(int quantity) {
        assertPositive(quantity);
        if (reservedQty < quantity) {
            throw new DomainException("已分配库存不足，无法发运");
        }
        reservedQty -= quantity;
        onHandQty -= quantity;
        version += 1;
        ensureQuantityInvariant();
    }

    /**
     * 上架完成后增加账面库存和可用库存。
     */
    public void increaseOnHand(int quantity) {
        assertPositive(quantity);
        onHandQty += quantity;
        availableQty += quantity;
        version += 1;
        ensureQuantityInvariant();
    }

    private void assertPositive(int quantity) {
        if (quantity <= 0) {
            throw new DomainException("库存数量必须大于 0");
        }
    }

    private void ensureQuantityInvariant() {
        if (onHandQty < 0 || availableQty < 0 || reservedQty < 0 || frozenQty < 0) {
            throw new DomainException("库存数量不能为负数");
        }
        if (onHandQty != availableQty + reservedQty + frozenQty) {
            throw new DomainException("库存数量不满足账面库存一致性约束");
        }
    }

    public Long getInventoryId() {
        return inventoryId;
    }

    public InventoryDimension getDimension() {
        return dimension;
    }

    public int getOnHandQty() {
        return onHandQty;
    }

    public int getAvailableQty() {
        return availableQty;
    }

    public int getReservedQty() {
        return reservedQty;
    }

    public int getFrozenQty() {
        return frozenQty;
    }

    public int getVersion() {
        return version;
    }
}
