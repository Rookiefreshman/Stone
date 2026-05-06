package com.stone.wms.application.service;

import com.stone.wms.application.assembler.OutboundOrderAssembler;
import com.stone.wms.application.dto.AllocateOutboundCommand;
import com.stone.wms.application.dto.OutboundOrderDto;
import com.stone.wms.domain.model.inventory.Inventory;
import com.stone.wms.domain.model.inventory.InventoryDimension;
import com.stone.wms.domain.model.inventory.InventoryStatus;
import com.stone.wms.domain.model.outbound.OutboundOrder;
import com.stone.wms.domain.model.outbound.OutboundOrderLine;
import com.stone.wms.domain.model.outbound.OutboundOrderStatus;
import com.stone.wms.domain.repository.InventoryRepository;
import com.stone.wms.domain.repository.OutboundOrderRepository;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutboundAllocationApplicationServiceTest {
    @Test
    void shouldAllocateOutboundOrderAndReserveInventory() {
        OutboundOrderRepository outboundOrderRepository = mock(OutboundOrderRepository.class);
        InventoryRepository inventoryRepository = mock(InventoryRepository.class);
        OutboundAllocationApplicationService service = new OutboundAllocationApplicationService(
                outboundOrderRepository, inventoryRepository, new OutboundOrderAssembler());
        OutboundOrder order = new OutboundOrder(1L, "SO-1", 1L, OutboundOrderStatus.APPROVED,
                Arrays.asList(new OutboundOrderLine(11L, 100L, 10L, "LOT-1", 3, 0)));
        Inventory inventory = new Inventory(21L,
                new InventoryDimension(1L, 10L, 100L, "LOT-1", InventoryStatus.AVAILABLE),
                10, 10, 0, 0, 0);

        when(outboundOrderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(inventoryRepository.findByDimension(any(InventoryDimension.class))).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(outboundOrderRepository.save(any(OutboundOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OutboundOrderDto dto = service.allocate(new AllocateOutboundCommand(1L, "idem-1"));

        assertThat(dto.getStatus()).isEqualTo("ALLOCATED");
        assertThat(dto.getLines().get(0).getAllocatedQty()).isEqualTo(3);
        assertThat(inventory.getAvailableQty()).isEqualTo(7);
        assertThat(inventory.getReservedQty()).isEqualTo(3);
        verify(inventoryRepository).save(inventory);
        verify(outboundOrderRepository).save(order);
    }
}
