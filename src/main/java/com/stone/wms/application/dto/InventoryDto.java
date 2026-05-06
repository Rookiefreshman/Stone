package com.stone.wms.application.dto;

/**
 * 库存 DTO 只用于应用层与接口层之间传输数据。
 */
public class InventoryDto {
    private Long inventoryId;
    private Long warehouseId;
    private Long locationId;
    private Long skuId;
    private String lotNo;
    private String inventoryStatus;
    private int onHandQty;
    private int availableQty;
    private int reservedQty;
    private int frozenQty;

    public Long getInventoryId() {
        return inventoryId;
    }

    public void setInventoryId(Long inventoryId) {
        this.inventoryId = inventoryId;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public Long getLocationId() {
        return locationId;
    }

    public void setLocationId(Long locationId) {
        this.locationId = locationId;
    }

    public Long getSkuId() {
        return skuId;
    }

    public void setSkuId(Long skuId) {
        this.skuId = skuId;
    }

    public String getLotNo() {
        return lotNo;
    }

    public void setLotNo(String lotNo) {
        this.lotNo = lotNo;
    }

    public String getInventoryStatus() {
        return inventoryStatus;
    }

    public void setInventoryStatus(String inventoryStatus) {
        this.inventoryStatus = inventoryStatus;
    }

    public int getOnHandQty() {
        return onHandQty;
    }

    public void setOnHandQty(int onHandQty) {
        this.onHandQty = onHandQty;
    }

    public int getAvailableQty() {
        return availableQty;
    }

    public void setAvailableQty(int availableQty) {
        this.availableQty = availableQty;
    }

    public int getReservedQty() {
        return reservedQty;
    }

    public void setReservedQty(int reservedQty) {
        this.reservedQty = reservedQty;
    }

    public int getFrozenQty() {
        return frozenQty;
    }

    public void setFrozenQty(int frozenQty) {
        this.frozenQty = frozenQty;
    }
}
