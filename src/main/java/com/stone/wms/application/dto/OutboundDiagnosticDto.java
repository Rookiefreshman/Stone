package com.stone.wms.application.dto;

import com.stone.wms.domain.repository.OutboundDiagnosticRepository.Row;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class OutboundDiagnosticDto {
    public final long warehouseId,orderId;
    public final String orderNo,orderStatus;
    public final boolean receiptPresent;
    public final List<Line> lines;
    public OutboundDiagnosticDto(List<Row> rows) {
        Row first=rows.get(0);
        warehouseId=first.warehouseId; orderId=first.orderId; orderNo=first.orderNo;
        orderStatus=first.orderStatus; receiptPresent=first.receiptPresent;
        List<Line> result=new ArrayList<>();
        for(Row row:rows) result.add(new Line(row));
        lines=Collections.unmodifiableList(result);
    }
    public static final class Line {
        public final Long lineId,locationId,skuId,inventoryId;
        public final String lotNo,locationState;
        public final Integer orderedQty,allocatedQty,onHandQty,availableQty,reservedQty,frozenQty,inventoryVersion;
        Line(Row r) {
            lineId=r.lineId; locationId=r.locationId; skuId=r.skuId; inventoryId=r.inventoryId;
            lotNo=r.lotNo; locationState=r.locationState; orderedQty=r.orderedQty; allocatedQty=r.allocatedQty;
            onHandQty=r.onHandQty; availableQty=r.availableQty; reservedQty=r.reservedQty; frozenQty=r.frozenQty;
            inventoryVersion=r.inventoryVersion;
        }
    }
}
