package com.stone.wms.infrastructure.mapper;

import com.stone.wms.domain.repository.OutboundDiagnosticRepository.Row;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

public interface OutboundDiagnosticMapper {
    // One consistent read of all requested dimensions, including unconfigured locations and missing inventory.
    @Select("SELECT o.warehouse_id,o.id AS order_id,o.order_no,o.status AS order_status,"
        + " (r.order_id IS NOT NULL) AS receipt_present,"
        + " l.id AS line_id,l.location_id,l.sku_id,l.lot_no,l.ordered_qty,l.allocated_qty,"
        + " i.id AS inventory_id,i.on_hand_qty,i.available_qty,i.reserved_qty,i.frozen_qty,i.version AS inventory_version,"
        + " CASE WHEN loc.location_id IS NULL THEN 'UNCONFIGURED' WHEN loc.freeze_key IS NOT NULL THEN 'FROZEN' ELSE 'OPEN' END AS location_state"
        + " FROM wms_outbound_order o"
        + " LEFT JOIN wms_outbound_order_line l ON l.order_id=o.id AND l.deleted=0"
        + " LEFT JOIN wms_inventory i ON i.warehouse_id=o.warehouse_id AND i.location_id=l.location_id"
        + " AND i.sku_id=l.sku_id AND i.lot_no=l.lot_no AND i.inventory_status='AVAILABLE' AND i.deleted=0"
        + " LEFT JOIN wms_work_location loc ON loc.warehouse_id=o.warehouse_id AND loc.location_id=l.location_id"
        + " LEFT JOIN wms_allocation_receipt r ON r.order_id=o.id"
        + " WHERE o.warehouse_id=#{warehouse} AND o.id=#{order} AND o.deleted=0"
        + " ORDER BY l.id LIMIT 201")
    List<Row> snapshot(@Param("warehouse") long warehouseId, @Param("order") long orderId);
}
