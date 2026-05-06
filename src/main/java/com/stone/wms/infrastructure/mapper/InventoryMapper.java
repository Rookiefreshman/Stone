package com.stone.wms.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stone.wms.infrastructure.entity.InventoryEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 库存 Mapper 只服务基础设施层，复杂 SQL 放在 XML 中维护。
 */
@Mapper
public interface InventoryMapper extends BaseMapper<InventoryEntity> {
    InventoryEntity selectByDimension(@Param("warehouseId") Long warehouseId,
                                      @Param("locationId") Long locationId,
                                      @Param("skuId") Long skuId,
                                      @Param("lotNo") String lotNo,
                                      @Param("inventoryStatus") String inventoryStatus);

    int updateQuantityWithVersion(@Param("entity") InventoryEntity entity, @Param("oldVersion") Integer oldVersion);
}
