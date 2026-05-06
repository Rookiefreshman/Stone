package com.stone.wms.interfaces.request;

import javax.validation.constraints.NotBlank;

/**
 * 出库分配请求对象只在接口层承接外部输入。
 */
public class AllocateOutboundRequest {
    @NotBlank(message = "幂等键不能为空")
    private String idempotencyKey;

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }
}
