package com.stone.wms.domain.model.outbound;

/**
 * 出库单状态机由领域模型控制，避免外层直接修改状态字段。
 */
public enum OutboundOrderStatus {
    DRAFT,
    APPROVED,
    ALLOCATED,
    PICKING,
    PICKED,
    CHECKED,
    SHIPPED,
    COMPLETED,
    CANCELLED,
    EXCEPTION
}
