package com.stone.wms.domain.repository;

import com.stone.wms.domain.model.inventory.Inventory;
import com.stone.wms.domain.model.inventory.InventoryDimension;

import java.util.Optional;

/**
 * 库存仓储接口由领域层定义，基础设施层负责实现持久化细节。
 */
public interface InventoryRepository {
    Optional<Inventory> findByDimension(InventoryDimension dimension);

    Inventory save(Inventory inventory);
}
