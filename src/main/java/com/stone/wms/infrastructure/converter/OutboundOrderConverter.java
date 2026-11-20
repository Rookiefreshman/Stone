package com.stone.wms.infrastructure.converter;

import com.stone.wms.domain.model.outbound.OutboundOrder;
import com.stone.wms.domain.model.outbound.OutboundOrderLine;
import com.stone.wms.domain.model.outbound.OutboundOrderStatus;
import com.stone.wms.infrastructure.entity.OutboundOrderEntity;
import com.stone.wms.infrastructure.entity.OutboundOrderLineEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * OutboundOrderConverter 只负责出库单 PO 与 BO 转换。
 */
@Component
public class OutboundOrderConverter {
    public OutboundOrder toDomain(OutboundOrderEntity orderEntity, List<OutboundOrderLineEntity> lineEntities) {
        List<OutboundOrderLine> lines = new ArrayList<OutboundOrderLine>();
        for (OutboundOrderLineEntity lineEntity : lineEntities) {
            lines.add(new OutboundOrderLine(lineEntity.getId(), lineEntity.getSkuId(), lineEntity.getLocationId(),
                    lineEntity.getLotNo(), safeInt(lineEntity.getOrderedQty()), safeInt(lineEntity.getAllocatedQty())));
        }
        return new OutboundOrder(orderEntity.getId(), orderEntity.getOrderNo(), orderEntity.getWarehouseId(),
                OutboundOrderStatus.valueOf(orderEntity.getStatus()), lines);
    }

    public OutboundOrderEntity toOrderEntity(OutboundOrder order) {
        OutboundOrderEntity entity = new OutboundOrderEntity();
        entity.setId(order.getOrderId());
        entity.setOrderNo(order.getOrderNo());
        entity.setWarehouseId(order.getWarehouseId());
        entity.setStatus(order.getStatus().name());
        entity.setDeleted(0);
        return entity;
    }

    public List<OutboundOrderLineEntity> toLineEntities(OutboundOrder order) {
        List<OutboundOrderLineEntity> entities = new ArrayList<OutboundOrderLineEntity>();
        for (OutboundOrderLine line : order.getLines()) {
            OutboundOrderLineEntity entity = new OutboundOrderLineEntity();
            entity.setId(line.getLineId());
            entity.setOrderId(order.getOrderId());
            entity.setSkuId(line.getSkuId());
            entity.setLocationId(line.getLocationId());
            entity.setLotNo(line.getLotNo());
            entity.setOrderedQty(line.getOrderedQty());
            entity.setAllocatedQty(line.getAllocatedQty());
            entity.setDeleted(0);
            entities.add(entity);
        }
        return entities;
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }
}
