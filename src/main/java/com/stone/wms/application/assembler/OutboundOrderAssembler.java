package com.stone.wms.application.assembler;

import org.springframework.stereotype.Component;

import com.stone.wms.application.dto.OutboundOrderDto;
import com.stone.wms.domain.model.outbound.OutboundOrder;
import com.stone.wms.domain.model.outbound.OutboundOrderLine;

import java.util.ArrayList;
import java.util.List;

/**
 * OutboundOrderAssembler 只负责出库单 BO 到 DTO 的转换。
 */
@Component
public class OutboundOrderAssembler {
    public OutboundOrderDto toDto(OutboundOrder order) {
        OutboundOrderDto dto = new OutboundOrderDto();
        dto.setOrderId(order.getOrderId());
        dto.setOrderNo(order.getOrderNo());
        dto.setWarehouseId(order.getWarehouseId());
        dto.setStatus(order.getStatus().name());
        List<OutboundOrderDto.OutboundOrderLineDto> lineDtos = new ArrayList<OutboundOrderDto.OutboundOrderLineDto>();
        for (OutboundOrderLine line : order.getLines()) {
            OutboundOrderDto.OutboundOrderLineDto lineDto = new OutboundOrderDto.OutboundOrderLineDto();
            lineDto.setLineId(line.getLineId());
            lineDto.setSkuId(line.getSkuId());
            lineDto.setLocationId(line.getLocationId());
            lineDto.setLotNo(line.getLotNo());
            lineDto.setOrderedQty(line.getOrderedQty());
            lineDto.setAllocatedQty(line.getAllocatedQty());
            lineDtos.add(lineDto);
        }
        dto.setLines(lineDtos);
        return dto;
    }
}
