package com.stone.wms.dto;

import java.util.List;
import java.util.stream.Collectors;

import com.stone.wms.domain.StockMovement;

public record InventoryResponse(
        String sku,
        int totalQuantity,
        List<LocationStockResponse> locations,
        List<StockMovement> movements
) {
    public String toJson() {
        return "{\"sku\":\"%s\",\"totalQuantity\":%d,\"locations\":%s,\"movements\":%s}"
                .formatted(
                        sku,
                        totalQuantity,
                        locations.stream().map(LocationStockResponse::toJson).collect(Collectors.toList()),
                        movements.stream().map(StockMovement::toJson).collect(Collectors.toList())
                );
    }
}
