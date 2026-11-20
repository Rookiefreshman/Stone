package com.stone.wms.domain.model.inventory;

import java.util.Objects;

/**
 * 库存维度定义库存余额唯一性，避免在应用层拼装业务唯一键。
 */
public class InventoryDimension {
    private final Long warehouseId;
    private final Long locationId;
    private final Long skuId;
    private final String lotNo;
    private final InventoryStatus inventoryStatus;

    public InventoryDimension(Long warehouseId, Long locationId, Long skuId, String lotNo,
                              InventoryStatus inventoryStatus) {
        this.warehouseId = Objects.requireNonNull(warehouseId, "warehouseId must not be null");
        this.locationId = Objects.requireNonNull(locationId, "locationId must not be null");
        this.skuId = Objects.requireNonNull(skuId, "skuId must not be null");
        this.lotNo = lotNo == null ? "" : lotNo;
        this.inventoryStatus = Objects.requireNonNull(inventoryStatus, "inventoryStatus must not be null");
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public Long getLocationId() {
        return locationId;
    }

    public Long getSkuId() {
        return skuId;
    }

    public String getLotNo() {
        return lotNo;
    }

    public InventoryStatus getInventoryStatus() {
        return inventoryStatus;
    }
}
