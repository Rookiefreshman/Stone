package com.stone.wms.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stone.wms.infrastructure.entity.OutboundOrderEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 出库单 Mapper 只服务基础设施层。
 */
@Mapper
public interface OutboundOrderMapper extends BaseMapper<OutboundOrderEntity> {
}
