package com.stone.wms.application.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 出库单 DTO 屏蔽领域模型内部状态机实现。
 */
public class OutboundOrderDto {
    private Long orderId;
    private String orderNo;
    private Long warehouseId;
    private String status;
    private List<OutboundOrderLineDto> lines = new ArrayList<OutboundOrderLineDto>();

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<OutboundOrderLineDto> getLines() {
        return Collections.unmodifiableList(lines);
    }

    public void setLines(List<OutboundOrderLineDto> lines) {
        this.lines = new ArrayList<OutboundOrderLineDto>(lines);
    }

    public static class OutboundOrderLineDto {
        private Long lineId;
        private Long skuId;
        private Long locationId;
        private String lotNo;
        private int orderedQty;
        private int allocatedQty;

        public Long getLineId() {
            return lineId;
        }

        public void setLineId(Long lineId) {
            this.lineId = lineId;
        }

        public Long getSkuId() {
            return skuId;
        }

        public void setSkuId(Long skuId) {
            this.skuId = skuId;
        }

        public Long getLocationId() {
            return locationId;
        }

        public void setLocationId(Long locationId) {
            this.locationId = locationId;
        }

        public String getLotNo() {
            return lotNo;
        }

        public void setLotNo(String lotNo) {
            this.lotNo = lotNo;
        }

        public int getOrderedQty() {
            return orderedQty;
        }

        public void setOrderedQty(int orderedQty) {
            this.orderedQty = orderedQty;
        }

        public int getAllocatedQty() {
            return allocatedQty;
        }

        public void setAllocatedQty(int allocatedQty) {
            this.allocatedQty = allocatedQty;
        }
    }
}
