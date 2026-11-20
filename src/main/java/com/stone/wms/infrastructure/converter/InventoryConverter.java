package com.stone.wms.infrastructure.converter;

import com.stone.wms.domain.model.inventory.Inventory;
import com.stone.wms.domain.model.inventory.InventoryDimension;
import com.stone.wms.domain.model.inventory.InventoryStatus;
import com.stone.wms.infrastructure.entity.InventoryEntity;
import org.springframework.stereotype.Component;

/**
 * InventoryConverter 只负责 PO 与 BO 转换，禁止转换 DTO。
 */
@Component
public class InventoryConverter {
    public Inventory toDomain(InventoryEntity entity) {
        InventoryDimension dimension = new InventoryDimension(entity.getWarehouseId(), entity.getLocationId(),
                entity.getSkuId(), entity.getLotNo(), InventoryStatus.valueOf(entity.getInventoryStatus()));
        return new Inventory(entity.getId(), dimension, safeInt(entity.getOnHandQty()), safeInt(entity.getAvailableQty()),
                safeInt(entity.getReservedQty()), safeInt(entity.getFrozenQty()), safeInt(entity.getVersion()));
    }

    public InventoryEntity toEntity(Inventory inventory) {
        InventoryEntity entity = new InventoryEntity();
        entity.setId(inventory.getInventoryId());
        entity.setWarehouseId(inventory.getDimension().getWarehouseId());
        entity.setLocationId(inventory.getDimension().getLocationId());
        entity.setSkuId(inventory.getDimension().getSkuId());
        entity.setLotNo(inventory.getDimension().getLotNo());
        entity.setInventoryStatus(inventory.getDimension().getInventoryStatus().name());
        entity.setOnHandQty(inventory.getOnHandQty());
        entity.setAvailableQty(inventory.getAvailableQty());
        entity.setReservedQty(inventory.getReservedQty());
        entity.setFrozenQty(inventory.getFrozenQty());
        entity.setVersion(inventory.getVersion());
        entity.setDeleted(0);
        return entity;
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }
}
