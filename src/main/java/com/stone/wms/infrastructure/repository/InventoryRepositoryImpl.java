package com.stone.wms.infrastructure.repository;

import com.stone.wms.domain.exception.DomainException;
import com.stone.wms.domain.model.inventory.Inventory;
import com.stone.wms.domain.model.inventory.InventoryDimension;
import com.stone.wms.domain.repository.InventoryRepository;
import com.stone.wms.infrastructure.converter.InventoryConverter;
import com.stone.wms.infrastructure.entity.InventoryEntity;
import com.stone.wms.infrastructure.mapper.InventoryMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 库存仓储实现负责将领域仓储接口适配到 MyBatis-Plus Mapper。
 */
@Repository
public class InventoryRepositoryImpl implements InventoryRepository {
    private final InventoryMapper inventoryMapper;
    private final InventoryConverter inventoryConverter;

    public InventoryRepositoryImpl(InventoryMapper inventoryMapper, InventoryConverter inventoryConverter) {
        this.inventoryMapper = inventoryMapper;
        this.inventoryConverter = inventoryConverter;
    }

    @Override
    public Optional<Inventory> findByDimension(InventoryDimension dimension) {
        InventoryEntity entity = inventoryMapper.selectByDimension(dimension.getWarehouseId(), dimension.getLocationId(),
                dimension.getSkuId(), dimension.getLotNo(), dimension.getInventoryStatus().name());
        return entity == null ? Optional.<Inventory>empty() : Optional.of(inventoryConverter.toDomain(entity));
    }

    @Override
    public Inventory save(Inventory inventory) {
        InventoryEntity entity = inventoryConverter.toEntity(inventory);
        int oldVersion = inventory.getVersion() - 1;
        int updated = inventoryMapper.updateQuantityWithVersion(entity, oldVersion);
        if (updated != 1) {
            throw new DomainException("库存并发更新失败，请重试");
        }
        return inventory;
    }
}
