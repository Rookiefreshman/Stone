package com.stone.wms.service;

import java.util.ArrayList;
import java.util.List;

import com.stone.wms.domain.InventoryItem;
import com.stone.wms.domain.MovementType;
import com.stone.wms.domain.StockMovement;
import com.stone.wms.dto.InventoryResponse;
import com.stone.wms.dto.LocationStockResponse;
import com.stone.wms.dto.ReceiveRequest;
import com.stone.wms.dto.ShipRequest;
import com.stone.wms.dto.ShipmentAllocationResponse;
import com.stone.wms.exception.InsufficientInventoryException;
import com.stone.wms.repository.InventoryRepository;
import com.stone.wms.repository.StockMovementRepository;

public class WarehouseService {
    private final InventoryRepository inventoryRepository;
    private final StockMovementRepository movementRepository;

    public WarehouseService(InventoryRepository inventoryRepository, StockMovementRepository movementRepository) {
        this.inventoryRepository = inventoryRepository;
        this.movementRepository = movementRepository;
    }

    public InventoryResponse receive(ReceiveRequest request) {
        InventoryItem item = inventoryRepository.findBySkuAndLocation(request.sku(), request.location())
                .orElseGet(() -> new InventoryItem(request.sku(), request.location(), 0));
        item.increase(request.quantity());
        inventoryRepository.save(item);
        movementRepository.save(StockMovement.create(
                MovementType.RECEIVE,
                item.getSku(),
                item.getLocation(),
                request.quantity(),
                request.referenceNo()
        ));
        return getInventory(request.sku());
    }

    public List<ShipmentAllocationResponse> ship(ShipRequest request) {
        List<InventoryItem> stocks = inventoryRepository.findBySku(request.sku()).stream()
                .filter(item -> item.getQuantity() > 0)
                .toList();
        int available = stocks.stream().mapToInt(InventoryItem::getQuantity).sum();
        if (available < request.quantity()) {
            throw new InsufficientInventoryException(request.sku(), request.quantity(), available);
        }

        int remaining = request.quantity();
        List<ShipmentAllocationResponse> allocations = new ArrayList<>();
        for (InventoryItem stock : stocks) {
            if (remaining == 0) {
                break;
            }
            int picked = Math.min(stock.getQuantity(), remaining);
            stock.decrease(picked);
            inventoryRepository.save(stock);
            movementRepository.save(StockMovement.create(
                    MovementType.SHIP,
                    stock.getSku(),
                    stock.getLocation(),
                    picked,
                    request.referenceNo()
            ));
            allocations.add(new ShipmentAllocationResponse(stock.getSku(), stock.getLocation(), picked, request.referenceNo()));
            remaining -= picked;
        }
        return allocations;
    }

    public InventoryResponse getInventory(String sku) {
        List<InventoryItem> items = inventoryRepository.findBySku(sku);
        List<LocationStockResponse> locations = items.stream()
                .map(item -> new LocationStockResponse(item.getLocation(), item.getQuantity(), item.getUpdatedAt()))
                .toList();
        int totalQuantity = items.stream().mapToInt(InventoryItem::getQuantity).sum();
        String normalizedSku = items.isEmpty() ? sku.trim().toUpperCase() : items.getFirst().getSku();
        return new InventoryResponse(normalizedSku, totalQuantity, locations, movementRepository.findBySku(normalizedSku));
    }
}
