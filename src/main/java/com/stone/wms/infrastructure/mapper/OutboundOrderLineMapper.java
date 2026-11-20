package com.stone.wms.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stone.wms.infrastructure.entity.OutboundOrderLineEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 出库单明细 Mapper 只服务基础设施层。
 */
@Mapper
public interface OutboundOrderLineMapper extends BaseMapper<OutboundOrderLineEntity> {
    List<OutboundOrderLineEntity> selectByOrderId(@Param("orderId") Long orderId);

    int updateAllocatedQty(@Param("id") Long id, @Param("allocatedQty") Integer allocatedQty);
}
