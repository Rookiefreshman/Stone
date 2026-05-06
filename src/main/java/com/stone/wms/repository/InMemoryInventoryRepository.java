package com.stone.wms.repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.stone.wms.domain.InventoryItem;

public class InMemoryInventoryRepository implements InventoryRepository {
    private final Map<String, InventoryItem> storage = new ConcurrentHashMap<>();

    @Override
    public Optional<InventoryItem> findBySkuAndLocation(String sku, String location) {
        return Optional.ofNullable(storage.get(key(sku, location)));
    }

    @Override
    public List<InventoryItem> findBySku(String sku) {
        String normalizedSku = normalize(sku);
        return storage.values().stream()
                .filter(item -> item.getSku().equals(normalizedSku))
                .sorted(Comparator.comparing(InventoryItem::getUpdatedAt).thenComparing(InventoryItem::getLocation))
                .toList();
    }

    @Override
    public InventoryItem save(InventoryItem item) {
        storage.put(key(item.getSku(), item.getLocation()), item);
        return item;
    }

    private static String key(String sku, String location) {
        return normalize(sku) + "@" + normalize(location);
    }

    private static String normalize(String value) {
        return value.trim().toUpperCase();
    }
}
