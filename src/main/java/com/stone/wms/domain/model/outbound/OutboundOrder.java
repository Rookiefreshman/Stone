package com.stone.wms.domain.model.outbound;

import com.stone.wms.domain.exception.DomainException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 出库单聚合根，负责单据状态机和明细完整性。
 */
public class OutboundOrder {
    private Long orderId;
    private String orderNo;
    private Long warehouseId;
    private OutboundOrderStatus status;
    private List<OutboundOrderLine> lines;

    public OutboundOrder(Long orderId, String orderNo, Long warehouseId, OutboundOrderStatus status,
                         List<OutboundOrderLine> lines) {
        if (warehouseId == null) {
            throw new DomainException("出库仓库不能为空");
        }
        if (lines == null || lines.isEmpty()) {
            throw new DomainException("出库单明细不能为空");
        }
        this.orderId = orderId;
        this.orderNo = orderNo;
        this.warehouseId = warehouseId;
        this.status = status == null ? OutboundOrderStatus.DRAFT : status;
        this.lines = new ArrayList<OutboundOrderLine>(lines);
    }

    /**
     * 只有审核后的出库单才能分配库存，保证状态机和库存动作一致。
     */
    public List<AllocationPlan> allocate() {
        if (status != OutboundOrderStatus.APPROVED) {
            throw new DomainException("只有已审核出库单可以分配库存");
        }
        List<AllocationPlan> plans = new ArrayList<AllocationPlan>();
        for (OutboundOrderLine line : lines) {
            plans.add(new AllocationPlan(warehouseId, line.getLocationId(), line.getSkuId(), line.getLotNo(),
                    line.allocateRemaining()));
        }
        status = OutboundOrderStatus.ALLOCATED;
        return plans;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public OutboundOrderStatus getStatus() {
        return status;
    }

    public List<OutboundOrderLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

    /**
     * 库存分配计划是出库单聚合对库存聚合发出的领域意图。
     */
    public static class AllocationPlan {
        private final Long warehouseId;
        private final Long locationId;
        private final Long skuId;
        private final String lotNo;
        private final int quantity;

        public AllocationPlan(Long warehouseId, Long locationId, Long skuId, String lotNo, int quantity) {
            this.warehouseId = warehouseId;
            this.locationId = locationId;
            this.skuId = skuId;
            this.lotNo = lotNo;
            this.quantity = quantity;
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

        public int getQuantity() {
            return quantity;
        }
    }
}
