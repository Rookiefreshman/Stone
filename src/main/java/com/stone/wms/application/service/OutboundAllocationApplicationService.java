package com.stone.wms.application.service;

import com.stone.wms.application.assembler.OutboundOrderAssembler;
import com.stone.wms.application.dto.AllocateOutboundCommand;
import com.stone.wms.application.dto.OutboundOrderDto;
import com.stone.wms.domain.exception.DomainException;
import com.stone.wms.domain.model.inventory.Inventory;
import com.stone.wms.domain.model.inventory.InventoryDimension;
import com.stone.wms.domain.model.inventory.InventoryStatus;
import com.stone.wms.domain.model.outbound.OutboundOrder;
import com.stone.wms.domain.repository.InventoryRepository;
import com.stone.wms.domain.repository.OutboundOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 出库分配应用服务负责事务和跨聚合编排，不直接访问 Mapper 或数据库实体。
 */
@Service
public class OutboundAllocationApplicationService {
    private final OutboundOrderRepository outboundOrderRepository;
    private final InventoryRepository inventoryRepository;
    private final OutboundOrderAssembler outboundOrderAssembler;

    public OutboundAllocationApplicationService(OutboundOrderRepository outboundOrderRepository,
                                                InventoryRepository inventoryRepository,
                                                OutboundOrderAssembler outboundOrderAssembler) {
        this.outboundOrderRepository = outboundOrderRepository;
        this.inventoryRepository = inventoryRepository;
        this.outboundOrderAssembler = outboundOrderAssembler;
    }

    /**
     * 一个出库分配用例对应一个事务边界，失败时库存和单据状态一起回滚。
     */
    @Transactional(rollbackFor = Exception.class)
    public OutboundOrderDto allocate(AllocateOutboundCommand command) {
        if (command == null || command.getOrderId() == null) {
            throw new DomainException("出库单 ID 不能为空");
        }
        OutboundOrder order = outboundOrderRepository.findById(command.getOrderId())
                .orElseThrow(() -> new DomainException("出库单不存在"));

        for (OutboundOrder.AllocationPlan plan : order.allocate()) {
            InventoryDimension dimension = new InventoryDimension(plan.getWarehouseId(), plan.getLocationId(),
                    plan.getSkuId(), plan.getLotNo(), InventoryStatus.AVAILABLE);
            Inventory inventory = inventoryRepository.findByDimension(dimension)
                    .orElseThrow(() -> new DomainException("库存不存在，无法分配"));
            inventory.reserve(plan.getQuantity());
            inventoryRepository.save(inventory);
        }

        OutboundOrder saved = outboundOrderRepository.save(order);
        return outboundOrderAssembler.toDto(saved);
    }
}
