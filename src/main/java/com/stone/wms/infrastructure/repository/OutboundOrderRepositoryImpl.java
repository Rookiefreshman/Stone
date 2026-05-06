package com.stone.wms.infrastructure.repository;

import com.stone.wms.domain.model.outbound.OutboundOrder;
import com.stone.wms.domain.repository.OutboundOrderRepository;
import com.stone.wms.infrastructure.converter.OutboundOrderConverter;
import com.stone.wms.infrastructure.entity.OutboundOrderEntity;
import com.stone.wms.infrastructure.entity.OutboundOrderLineEntity;
import com.stone.wms.infrastructure.mapper.OutboundOrderLineMapper;
import com.stone.wms.infrastructure.mapper.OutboundOrderMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 出库单仓储实现负责组合单据主表和明细表。
 */
@Repository
public class OutboundOrderRepositoryImpl implements OutboundOrderRepository {
    private final OutboundOrderMapper outboundOrderMapper;
    private final OutboundOrderLineMapper outboundOrderLineMapper;
    private final OutboundOrderConverter outboundOrderConverter;

    public OutboundOrderRepositoryImpl(OutboundOrderMapper outboundOrderMapper,
                                       OutboundOrderLineMapper outboundOrderLineMapper,
                                       OutboundOrderConverter outboundOrderConverter) {
        this.outboundOrderMapper = outboundOrderMapper;
        this.outboundOrderLineMapper = outboundOrderLineMapper;
        this.outboundOrderConverter = outboundOrderConverter;
    }

    @Override
    public Optional<OutboundOrder> findById(Long orderId) {
        OutboundOrderEntity orderEntity = outboundOrderMapper.selectById(orderId);
        if (orderEntity == null || Integer.valueOf(1).equals(orderEntity.getDeleted())) {
            return Optional.empty();
        }
        List<OutboundOrderLineEntity> lineEntities = outboundOrderLineMapper.selectByOrderId(orderId);
        return Optional.of(outboundOrderConverter.toDomain(orderEntity, lineEntities));
    }

    @Override
    public OutboundOrder save(OutboundOrder order) {
        outboundOrderMapper.updateById(outboundOrderConverter.toOrderEntity(order));
        for (OutboundOrderLineEntity lineEntity : outboundOrderConverter.toLineEntities(order)) {
            outboundOrderLineMapper.updateAllocatedQty(lineEntity.getId(), lineEntity.getAllocatedQty());
        }
        return order;
    }
}
