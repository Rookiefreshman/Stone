package com.stone.wms.application.assembler;

import org.springframework.stereotype.Component;

import com.stone.wms.application.dto.InventoryDto;
import com.stone.wms.domain.model.inventory.Inventory;

/**
 * InventoryAssembler 只负责 BO 到 DTO 的转换。
 */
@Component
public class InventoryAssembler {
    public InventoryDto toDto(Inventory inventory) {
        InventoryDto dto = new InventoryDto();
        dto.setInventoryId(inventory.getInventoryId());
        dto.setWarehouseId(inventory.getDimension().getWarehouseId());
        dto.setLocationId(inventory.getDimension().getLocationId());
        dto.setSkuId(inventory.getDimension().getSkuId());
        dto.setLotNo(inventory.getDimension().getLotNo());
        dto.setInventoryStatus(inventory.getDimension().getInventoryStatus().name());
        dto.setOnHandQty(inventory.getOnHandQty());
        dto.setAvailableQty(inventory.getAvailableQty());
        dto.setReservedQty(inventory.getReservedQty());
        dto.setFrozenQty(inventory.getFrozenQty());
        return dto;
    }
}
