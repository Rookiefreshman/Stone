package com.stone.wms.repository;

import java.util.List;

import com.stone.wms.domain.StockMovement;

public interface StockMovementRepository {
    StockMovement save(StockMovement movement);

    List<StockMovement> findBySku(String sku);
}
