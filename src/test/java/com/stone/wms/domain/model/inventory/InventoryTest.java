package com.stone.wms.domain.model.inventory;

import com.stone.wms.domain.exception.DomainException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InventoryTest {
    @Test
    void shouldReserveAvailableInventory() {
        Inventory inventory = new Inventory(1L,
                new InventoryDimension(1L, 10L, 100L, "LOT-1", InventoryStatus.AVAILABLE),
                10, 10, 0, 0, 0);

        inventory.reserve(3);

        assertThat(inventory.getOnHandQty()).isEqualTo(10);
        assertThat(inventory.getAvailableQty()).isEqualTo(7);
        assertThat(inventory.getReservedQty()).isEqualTo(3);
        assertThat(inventory.getVersion()).isEqualTo(1);
    }

    @Test
    void shouldRejectReserveWhenAvailableInventoryIsNotEnough() {
        Inventory inventory = new Inventory(1L,
                new InventoryDimension(1L, 10L, 100L, "LOT-1", InventoryStatus.AVAILABLE),
                10, 2, 8, 0, 0);

        assertThrows(DomainException.class, () -> inventory.reserve(3));
    }

    @Test
    void shouldRejectInvalidInvariant() {
        assertThrows(DomainException.class, () -> new Inventory(1L,
                new InventoryDimension(1L, 10L, 100L, "LOT-1", InventoryStatus.AVAILABLE),
                10, 9, 0, 0, 0));
    }
}
