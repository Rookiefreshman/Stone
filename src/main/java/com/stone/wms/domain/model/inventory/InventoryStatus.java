package com.stone.wms.domain.model.inventory;

/**
 * 库存状态用于隔离正常、冻结和残次等不同可用性库存。
 */
public enum InventoryStatus {
    AVAILABLE,
    FROZEN,
    DEFECTIVE
}
