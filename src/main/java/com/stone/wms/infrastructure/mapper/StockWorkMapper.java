package com.stone.wms.infrastructure.mapper;

import com.stone.wms.domain.model.inventory.StockWork;
import com.stone.wms.domain.repository.StockWorkRepository.Location;
import com.stone.wms.domain.repository.StockWorkRepository.LedgerRow;
import com.stone.wms.domain.repository.StockWorkRepository.Receipt;
import org.apache.ibatis.annotations.*;

import java.util.List;

public interface StockWorkMapper {
    @Select("SELECT location_id, biz_type AS action, quantity AS delta_on_hand, delta_available_qty AS delta_available,"
            + " delta_reserved_qty AS delta_reserved, delta_frozen_qty AS delta_frozen FROM wms_inventory_transaction"
            + " WHERE warehouse_id=#{warehouse} AND work_task_id=#{id} AND deleted=0 ORDER BY id")
    List<LedgerRow> ledger(@Param("warehouse") long warehouse, @Param("id") long taskId);

    @Select("SELECT max_units, mix_policy, freeze_key FROM wms_work_location"
            + " WHERE warehouse_id = #{warehouse} AND location_id = #{location} FOR UPDATE")
    Location lockLocation(@Param("warehouse") long warehouse, @Param("location") long location);

    @Select("SELECT COALESCE(SUM(on_hand_qty),0) FROM wms_inventory"
            + " WHERE warehouse_id=#{warehouse} AND location_id=#{location} AND deleted=0")
    long onHand(@Param("warehouse") long warehouse, @Param("location") long location);

    String ACTIVE = " JOIN wes_dispatch_task t ON t.id=w.task_id AND t.state IN ('READY','IN_FLIGHT','UNKNOWN')";

    @Select("SELECT COALESCE(SUM(w.quantity),0) FROM wms_stock_work w" + ACTIVE
            + " WHERE w.warehouse_id=#{warehouse} AND w.destination_id=#{location}")
    long incoming(@Param("warehouse") long warehouse, @Param("location") long location);

    @Select({"<script>", "SELECT EXISTS (SELECT 1 FROM wms_inventory i WHERE i.warehouse_id=#{warehouse}",
            "AND i.location_id=#{work.destinationId} AND i.deleted=0 AND i.on_hand_qty &gt; 0",
            "AND (i.sku_id != #{work.skuId} <if test='batch'>OR i.lot_no != #{work.lotNo}</if>))",
            "OR EXISTS (SELECT 1 FROM wms_stock_work w", ACTIVE,
            "WHERE w.warehouse_id=#{warehouse} AND w.destination_id=#{work.destinationId}",
            "AND (w.sku_id != #{work.skuId} <if test='batch'>OR w.lot_no != #{work.lotNo}</if>))", "</script>"})
    boolean hasMixConflict(@Param("warehouse") long warehouse, @Param("work") StockWork work, @Param("batch") boolean batch);

    @Select("SELECT EXISTS (SELECT 1 FROM wms_inventory WHERE warehouse_id=#{warehouse}"
            + " AND location_id=#{location} AND deleted=0 AND reserved_qty>0)"
            + " OR EXISTS (SELECT 1 FROM wms_stock_work w" + ACTIVE
            + " WHERE w.warehouse_id=#{warehouse} AND w.destination_id=#{location})"
            + " OR EXISTS (SELECT 1 FROM wms_stock_work w" + ACTIVE
            + " WHERE w.warehouse_id=#{warehouse} AND w.source_id=#{location})")
    boolean hasActiveStockOrWork(@Param("warehouse") long warehouse, @Param("location") long location);

    @Select("SELECT reason FROM wms_location_freeze WHERE warehouse_id=#{warehouse}"
            + " AND location_id=#{location} AND freeze_key=#{key}")
    String freezeReason(@Param("warehouse") long warehouse, @Param("location") long location, @Param("key") String key);

    @Insert("INSERT INTO wms_location_freeze (warehouse_id,location_id,freeze_key,reason)"
            + " VALUES (#{warehouse},#{location},#{key},#{reason})")
    void insertFreeze(@Param("warehouse") long warehouse, @Param("location") long location, @Param("key") String key,
                      @Param("reason") String reason);

    @Update("UPDATE wms_work_location SET freeze_key=#{key,jdbcType=VARCHAR}"
            + " WHERE warehouse_id=#{warehouse} AND location_id=#{location}")
    void setFreeze(@Param("warehouse") long warehouse, @Param("location") long location, @Param("key") String key);

    @Update("UPDATE wms_location_freeze SET released_at=NOW(6)"
            + " WHERE warehouse_id=#{warehouse} AND location_id=#{location} AND freeze_key=#{key}")
    void closeFreeze(@Param("warehouse") long warehouse, @Param("location") long location, @Param("key") String key);

    @Select("SELECT sku_id,lot_no,received_qty FROM wms_received_line WHERE warehouse_id=#{warehouse} AND receipt_key=#{key}")
    Receipt receipt(@Param("warehouse") long warehouse, @Param("key") String key);

    @Insert("INSERT INTO wms_received_line (warehouse_id,receipt_key,sku_id,lot_no,received_qty)"
            + " VALUES (#{warehouse},#{key},#{sku},#{lot},#{qty})")
    void insertReceipt(@Param("warehouse") long warehouse, @Param("key") String key, @Param("sku") long sku,
                       @Param("lot") String lot, @Param("qty") int quantity);

    @Select("SELECT COALESCE(SUM(w.quantity),0) FROM wms_stock_work w JOIN wes_dispatch_task t ON t.id=w.task_id"
            + " WHERE w.warehouse_id=#{warehouse} AND w.receipt_key=#{key} AND t.state NOT IN ('FAILED','CANCELLED')")
    long committedReceipt(@Param("warehouse") long warehouse, @Param("key") String key);

    @Insert("INSERT INTO wms_stock_work (task_id,warehouse_id,work_type,receipt_key,sku_id,lot_no,source_id,destination_id,quantity)"
            + " VALUES (#{id},#{warehouse},#{work.type},#{work.receiptKey,jdbcType=VARCHAR},#{work.skuId},#{work.lotNo},"
            + "#{work.sourceId,jdbcType=BIGINT},#{work.destinationId},#{work.quantity})")
    void insertWork(@Param("id") long taskId, @Param("warehouse") long warehouse, @Param("work") StockWork work);
}
