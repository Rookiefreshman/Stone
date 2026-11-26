package com.stone.wms.domain.repository;

import java.util.List;

/** Read projection of the actual allocation dimensions; never reserves inventory. */
public interface OutboundDiagnosticRepository {
    class Row {
        public long warehouseId, orderId;
        public String orderNo, orderStatus;
        public boolean receiptPresent;
        public Long lineId, locationId, skuId, inventoryId;
        public String lotNo, locationState;
        public Integer orderedQty, allocatedQty, onHandQty, availableQty, reservedQty, frozenQty, inventoryVersion;
    }
    List<Row> snapshot(long warehouseId, long orderId);
}
