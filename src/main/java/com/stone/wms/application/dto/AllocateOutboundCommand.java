package com.stone.wms.application.dto;

/**
 * 出库分配命令表达一个完整应用用例的输入。
 */
public class AllocateOutboundCommand {
    private final Long orderId;
    private final String idempotencyKey;

    public AllocateOutboundCommand(Long orderId, String idempotencyKey) {
        this.orderId = orderId;
        this.idempotencyKey = idempotencyKey;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }
}
