package com.stone.wms.repository;

import java.util.List;
import java.util.Optional;

import com.stone.wms.domain.InventoryItem;

public interface InventoryRepository {
    Optional<InventoryItem> findBySkuAndLocation(String sku, String location);

    List<InventoryItem> findBySku(String sku);

    InventoryItem save(InventoryItem item);
}
