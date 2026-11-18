package com.stone.wms.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stone.wms.infrastructure.entity.OutboundOrderEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 出库单 Mapper 只服务基础设施层。
 */
@Mapper
public interface OutboundOrderMapper extends BaseMapper<OutboundOrderEntity> {
    @Select("SELECT * FROM wms_outbound_order WHERE id = #{id} AND deleted = 0 FOR UPDATE")
    OutboundOrderEntity lockById(@Param("id") Long id);

    @Select("SELECT idempotency_key FROM wms_allocation_receipt WHERE order_id = #{id}")
    String allocationKey(@Param("id") Long id);

    @Insert("INSERT INTO wms_allocation_receipt (order_id, idempotency_key) VALUES (#{id}, #{key})")
    void recordAllocation(@Param("id") Long id, @Param("key") String key);
}
