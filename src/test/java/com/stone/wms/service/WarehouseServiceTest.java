package com.stone.wms.service;

import java.util.List;
import java.util.Objects;

import com.stone.wms.domain.MovementType;
import com.stone.wms.dto.InventoryResponse;
import com.stone.wms.dto.ReceiveRequest;
import com.stone.wms.dto.ShipRequest;
import com.stone.wms.dto.ShipmentAllocationResponse;
import com.stone.wms.exception.InsufficientInventoryException;
import com.stone.wms.repository.InMemoryInventoryRepository;
import com.stone.wms.repository.InMemoryStockMovementRepository;

class WarehouseServiceTest {
    public static void main(String[] args) {
        WarehouseServiceTest test = new WarehouseServiceTest();
        test.receiveShouldIncreaseInventoryAndRecordMovement();
        test.shipShouldAllocateAcrossLocationsAndRecordMovements();
        test.shipShouldRejectInsufficientInventory();
        System.out.println("WarehouseServiceTest passed");
    }

    void receiveShouldIncreaseInventoryAndRecordMovement() {
        WarehouseService warehouseService = newService();
        InventoryResponse response = warehouseService.receive(new ReceiveRequest("sku-001", "a-01-01", 12, "ASN-1"));

        assertEquals("SKU-001", response.sku());
        assertEquals(12, response.totalQuantity());
        assertEquals(1, response.locations().size());
        assertEquals("A-01-01", response.locations().getFirst().location());
        assertEquals(1, response.movements().size());
        assertEquals(MovementType.RECEIVE, response.movements().getFirst().type());
    }

    void shipShouldAllocateAcrossLocationsAndRecordMovements() {
        WarehouseService warehouseService = newService();
        warehouseService.receive(new ReceiveRequest("SKU-001", "A-01-01", 5, "ASN-1"));
        warehouseService.receive(new ReceiveRequest("SKU-001", "A-01-02", 7, "ASN-2"));

        List<ShipmentAllocationResponse> allocations = warehouseService.ship(new ShipRequest("SKU-001", 8, "SO-1"));
        InventoryResponse inventory = warehouseService.getInventory("SKU-001");

        assertEquals(2, allocations.size());
        assertEquals(5, allocations.get(0).quantity());
        assertEquals(3, allocations.get(1).quantity());
        assertEquals(4, inventory.totalQuantity());
        assertEquals(4, inventory.movements().size());
        long shipMovements = inventory.movements().stream().filter(movement -> movement.type() == MovementType.SHIP).count();
        assertEquals(2L, shipMovements);
    }

    void shipShouldRejectInsufficientInventory() {
        WarehouseService warehouseService = newService();
        warehouseService.receive(new ReceiveRequest("SKU-001", "A-01-01", 2, "ASN-1"));

        try {
            warehouseService.ship(new ShipRequest("SKU-001", 3, "SO-1"));
            throw new AssertionError("Expected InsufficientInventoryException");
        } catch (InsufficientInventoryException ex) {
            if (!ex.getMessage().contains("requested=3, available=2")) {
                throw new AssertionError("Unexpected exception message: " + ex.getMessage());
            }
        }
    }

    private static WarehouseService newService() {
        return new WarehouseService(new InMemoryInventoryRepository(), new InMemoryStockMovementRepository());
    }

    private static void assertEquals(Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("Expected %s but got %s".formatted(expected, actual));
        }
    }
}
