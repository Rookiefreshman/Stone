package com.stone.wms.domain.model.outbound;

import com.stone.wms.domain.exception.DomainException;

/**
 * 出库单明细记录 SKU 与待出库数量。
 */
public class OutboundOrderLine {
    private Long lineId;
    private Long skuId;
    private Long locationId;
    private String lotNo;
    private int orderedQty;
    private int allocatedQty;

    public OutboundOrderLine(Long lineId, Long skuId, Long locationId, String lotNo, int orderedQty, int allocatedQty) {
        if (skuId == null || locationId == null) {
            throw new DomainException("出库明细 SKU 和库位不能为空");
        }
        if (orderedQty <= 0 || allocatedQty < 0 || allocatedQty > orderedQty) {
            throw new DomainException("出库明细数量不合法");
        }
        this.lineId = lineId;
        this.skuId = skuId;
        this.locationId = locationId;
        this.lotNo = lotNo == null ? "" : lotNo;
        this.orderedQty = orderedQty;
        this.allocatedQty = allocatedQty;
    }

    /**
     * 分配剩余数量，确保重复分配不会突破单据需求量。
     */
    public int allocateRemaining() {
        int quantity = orderedQty - allocatedQty;
        if (quantity <= 0) {
            throw new DomainException("出库明细已完成分配");
        }
        allocatedQty = orderedQty;
        return quantity;
    }

    public Long getLineId() {
        return lineId;
    }

    public Long getSkuId() {
        return skuId;
    }

    public Long getLocationId() {
        return locationId;
    }

    public String getLotNo() {
        return lotNo;
    }

    public int getOrderedQty() {
        return orderedQty;
    }

    public int getAllocatedQty() {
        return allocatedQty;
    }
}
