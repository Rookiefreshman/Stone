package com.stone.wms.repository;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.stone.wms.domain.StockMovement;

public class InMemoryStockMovementRepository implements StockMovementRepository {
    private final CopyOnWriteArrayList<StockMovement> movements = new CopyOnWriteArrayList<>();

    @Override
    public StockMovement save(StockMovement movement) {
        movements.add(movement);
        return movement;
    }

    @Override
    public List<StockMovement> findBySku(String sku) {
        String normalizedSku = sku.trim().toUpperCase();
        return movements.stream()
                .filter(movement -> movement.sku().equals(normalizedSku))
                .sorted(Comparator.comparing(StockMovement::occurredAt))
                .toList();
    }
}
