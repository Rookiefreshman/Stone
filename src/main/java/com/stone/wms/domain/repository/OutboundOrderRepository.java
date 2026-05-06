package com.stone.wms.domain.repository;

import com.stone.wms.domain.model.outbound.OutboundOrder;

import java.util.Optional;

/**
 * 出库单仓储接口隔离应用层与具体数据库访问实现。
 */
public interface OutboundOrderRepository {
    Optional<OutboundOrder> findById(Long orderId);

    OutboundOrder save(OutboundOrder order);
}
