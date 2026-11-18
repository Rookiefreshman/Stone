package com.stone.warehouse.infrastructure;

import com.stone.warehouse.domain.WarehouseRepository;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.util.List;

import static com.stone.warehouse.domain.WarehouseModel.*;

@Mapper
public interface WarehouseMapper extends WarehouseRepository {
    @Select("SELECT * FROM warehouse ORDER BY code")
    List<Master> warehouses();

    @Select("SELECT * FROM warehouse WHERE id = #{id} FOR UPDATE")
    Master lockWarehouse(String id);

    @Insert("INSERT INTO warehouse (id, code, name) VALUES (#{id}, #{code}, #{name})")
    void insertWarehouse(Master master);

    @Select("SELECT * FROM sku WHERE warehouse_id = #{warehouseId} ORDER BY code")
    List<Master> skus(String warehouseId);

    @Select("SELECT * FROM sku WHERE id = #{id}")
    Master sku(String id);

    @Insert("INSERT INTO sku (id, warehouse_id, code, name) VALUES (#{id}, #{warehouseId}, #{code}, #{name})")
    void insertSku(Master master);

    @Select("SELECT * FROM location WHERE warehouse_id = #{warehouseId} ORDER BY code")
    List<Master> locations(String warehouseId);

    @Select("SELECT * FROM location WHERE id = #{id}")
    Master location(String id);

    @Insert("INSERT INTO location (id, warehouse_id, code, name, location_type)"
            + " VALUES (#{id}, #{warehouseId}, #{code}, #{name}, #{locationType})")
    void insertLocation(Master master);

    @Select("SELECT * FROM device WHERE warehouse_id = #{warehouseId} ORDER BY code")
    List<Master> devices(String warehouseId);

    @Select("SELECT * FROM device WHERE id = #{id}")
    Master device(String id);

    @Insert("INSERT INTO device (id, warehouse_id, code, name, device_mode, state)"
            + " VALUES (#{id}, #{warehouseId}, #{code}, #{name}, #{deviceMode}, #{state})")
    void insertDevice(Master master);

    @Update("UPDATE device SET state = #{state} WHERE id = #{id}")
    void setDeviceState(@Param("id") String id, @Param("state") DeviceState state);

    @Select("SELECT * FROM lot WHERE sku_id = #{skuId} AND batch_code = #{batchCode}")
    Lot lot(@Param("skuId") String skuId, @Param("batchCode") String batchCode);

    @Select("SELECT * FROM lot WHERE id = #{id}")
    Lot lotById(String id);

    @Insert("INSERT INTO lot (id, sku_id, batch_code, expires_on) VALUES (#{id}, #{skuId}, #{batchCode}, #{expiresOn})")
    void insertLot(Lot lot);

    String STOCK_VIEW = "SELECT s.*, l.code AS location_code, l.location_type, b.sku_id, b.batch_code, b.expires_on, k.code AS sku_code"
            + " FROM stock s JOIN lot b ON b.id = s.lot_id JOIN location l ON l.id = s.location_id"
            + " JOIN sku k ON k.id = b.sku_id";

    @Select(STOCK_VIEW + " WHERE l.warehouse_id = #{warehouseId}"
            + " ORDER BY l.code, k.code, b.batch_code LIMIT #{limit} OFFSET #{offset}")
    List<Stock> stocks(@Param("warehouseId") String warehouseId, @Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT * FROM stock WHERE location_id = #{locationId} AND lot_id = #{lotId}")
    Stock stock(@Param("locationId") String locationId, @Param("lotId") String lotId);

    @Select({"<script>", STOCK_VIEW, "WHERE b.sku_id = #{skuId} AND l.location_type != 'QUARANTINE'",
            "AND (b.expires_on IS NULL OR b.expires_on &gt;= #{today}) AND s.on_hand &gt; s.reserved",
            "<if test='lotId != null'>AND b.id = #{lotId}</if>",
            "<if test='sourceId != null'>AND l.id = #{sourceId}</if>",
            "ORDER BY CASE WHEN b.expires_on IS NULL THEN 1 ELSE 0 END, b.expires_on, b.batch_code, l.code", "</script>"})
    List<Stock> candidates(@Param("skuId") String skuId, @Param("lotId") String lotId,
                           @Param("sourceId") String sourceId, @Param("today") LocalDate today);

    @Insert("INSERT INTO stock (location_id, lot_id, on_hand, reserved) VALUES (#{locationId}, #{lotId}, #{onHand}, #{reserved})")
    void insertStock(Stock stock);

    @Update("UPDATE stock SET on_hand = #{onHand}, reserved = #{reserved} WHERE location_id = #{locationId} AND lot_id = #{lotId}")
    void updateStock(Stock stock);

    @Insert("INSERT INTO inventory_ledger (id, warehouse_id, task_id, location_id, lot_id, reason, delta_on_hand, delta_reserved)"
            + " VALUES (#{id}, #{warehouseId}, #{taskId}, #{locationId}, #{lotId}, #{reason}, #{deltaOnHand}, #{deltaReserved})")
    void insertLedger(Ledger ledger);

    @Select("SELECT a.*, b.batch_code FROM inventory_ledger a JOIN lot b ON b.id = a.lot_id WHERE a.warehouse_id = #{warehouseId}"
            + " ORDER BY a.created_at DESC, a.id LIMIT #{limit} OFFSET #{offset}")
    List<Ledger> ledger(@Param("warehouseId") String warehouseId, @Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT * FROM document WHERE id = #{id}")
    Document document(String id);

    @Select("SELECT * FROM document WHERE warehouse_id = #{warehouseId} AND kind = #{kind} AND external_ref = #{externalRef}")
    Document documentByRef(@Param("warehouseId") String warehouseId, @Param("kind") Kind kind,
                           @Param("externalRef") String externalRef);

    @Select("SELECT * FROM document WHERE warehouse_id = #{warehouseId}"
            + " ORDER BY created_at DESC, id LIMIT #{limit} OFFSET #{offset}")
    List<Document> documents(@Param("warehouseId") String warehouseId, @Param("limit") int limit, @Param("offset") int offset);

    @Insert("INSERT INTO document (id, warehouse_id, kind, external_ref, source_document_id, state, request_hash)"
            + " VALUES (#{id}, #{warehouseId}, #{kind}, #{externalRef}, #{sourceDocumentId}, #{state}, #{requestHash})")
    void insertDocument(Document document);

    @Update("UPDATE document SET state = #{state} WHERE id = #{id}")
    void setDocumentState(@Param("id") String id, @Param("state") DocumentState state);

    @Insert("INSERT INTO document_line (id, document_id, line_no, sku_id, lot_id, source_id, destination_id, quantity)"
            + " VALUES (#{id}, #{documentId}, #{lineNo}, #{skuId}, #{lotId}, #{sourceId}, #{destinationId}, #{quantity})")
    void insertLine(Line line);

    @Select("SELECT a.*, b.batch_code FROM document_line a LEFT JOIN lot b ON b.id = a.lot_id WHERE a.document_id = #{documentId} ORDER BY a.line_no")
    List<Line> lines(String documentId);

    @Select("SELECT COALESCE(SUM(quantity), 0) FROM task WHERE document_id = #{documentId} AND lot_id = #{lotId} AND state = 'DONE'")
    long shipped(@Param("documentId") String documentId, @Param("lotId") String lotId);

    @Select("SELECT COALESCE(SUM(l.quantity), 0) FROM document_line l JOIN document d ON d.id = l.document_id"
            + " WHERE d.source_document_id = #{sourceDocumentId} AND l.lot_id = #{lotId} AND d.state != 'CANCELLED'")
    long returning(@Param("sourceDocumentId") String sourceDocumentId, @Param("lotId") String lotId);

    @Select("SELECT * FROM outbound_wave WHERE id = #{id}")
    Wave wave(String id);

    @Select("SELECT w.* FROM outbound_wave w JOIN wave_document wd ON wd.wave_id = w.id"
            + " WHERE wd.document_id = #{documentId} AND w.state != 'CANCELLED'")
    Wave activeWave(String documentId);

    @Select("SELECT * FROM outbound_wave WHERE warehouse_id = #{warehouseId} ORDER BY created_at DESC, id"
            + " LIMIT #{limit} OFFSET #{offset}")
    List<Wave> waves(@Param("warehouseId") String warehouseId, @Param("limit") int limit, @Param("offset") int offset);

    @Insert("INSERT INTO outbound_wave (id, warehouse_id, code, priority, state)"
            + " VALUES (#{id}, #{warehouseId}, #{code}, #{priority}, #{state})")
    void insertWave(Wave wave);

    @Insert("INSERT INTO wave_document (wave_id, document_id, sequence_no) VALUES (#{waveId}, #{documentId}, #{sequenceNo})")
    void addWaveDocument(@Param("waveId") String waveId, @Param("documentId") String documentId,
                         @Param("sequenceNo") int sequenceNo);

    @Select("SELECT d.* FROM document d JOIN wave_document wd ON wd.document_id = d.id"
            + " WHERE wd.wave_id = #{waveId} ORDER BY wd.sequence_no")
    List<Document> waveDocuments(String waveId);

    @Update("UPDATE outbound_wave SET state = #{state}, reason = #{reason} WHERE id = #{id}")
    void updateWave(Wave wave);

    String TASK_VIEW = "SELECT t.*, b.batch_code FROM task t JOIN lot b ON b.id = t.lot_id";

    @Select(TASK_VIEW + " WHERE t.id = #{id}")
    Task task(String id);

    @Select(TASK_VIEW + " WHERE t.document_id = #{documentId} ORDER BY t.line_id, t.lot_id, t.source_id, t.id")
    List<Task> tasks(String documentId);

    @Select(TASK_VIEW + " LEFT JOIN (SELECT wd.document_id, w.priority FROM wave_document wd JOIN outbound_wave w"
            + " ON w.id = wd.wave_id WHERE w.state = 'RELEASED') wp ON wp.document_id = t.document_id"
            + " WHERE t.warehouse_id = #{warehouseId} AND t.state IN ('READY','RUNNING','EXCEPTION')"
            + " ORDER BY CASE WHEN t.state = 'EXCEPTION' THEN 0 WHEN t.state = 'RUNNING' THEN 1 ELSE 2 END,"
            + " COALESCE(wp.priority, 0) DESC, t.created_at, t.id LIMIT #{limit} OFFSET #{offset}")
    List<Task> queue(@Param("warehouseId") String warehouseId, @Param("limit") int limit, @Param("offset") int offset);

    @Insert("INSERT INTO task (id, warehouse_id, document_id, line_id, lot_id, source_id, destination_id, quantity, state)"
            + " VALUES (#{id}, #{warehouseId}, #{documentId}, #{lineId}, #{lotId}, #{sourceId}, #{destinationId}, #{quantity}, #{state})")
    void insertTask(Task task);

    @Update("UPDATE task SET state = #{state}, attempt = #{attempt}, observed_quantity = #{observedQuantity} WHERE id = #{id}")
    void updateTask(Task task);

    String ACTIVE_TASKS = " FROM task t JOIN document d ON d.id = t.document_id"
            + " WHERE t.warehouse_id = #{warehouseId} AND t.lot_id = #{lotId}"
            + " AND t.state != 'CANCELLED' AND d.state NOT IN ('COMPLETED','CANCELLED')";

    @Select("SELECT COUNT(*)" + ACTIVE_TASKS + " AND d.kind = 'COUNT' AND t.source_id = #{locationId}")
    int activeCount(@Param("warehouseId") String warehouseId, @Param("locationId") String locationId, @Param("lotId") String lotId);

    @Select("SELECT COUNT(*)" + ACTIVE_TASKS + " AND (t.source_id = #{locationId} OR t.destination_id = #{locationId})")
    int activeWork(@Param("warehouseId") String warehouseId, @Param("locationId") String locationId, @Param("lotId") String lotId);

    @Select("SELECT * FROM device_command WHERE id = #{id}")
    Command command(String id);

    @Select("SELECT * FROM device_command WHERE task_id = #{taskId} ORDER BY attempt")
    List<Command> commands(String taskId);

    @Select("SELECT COUNT(*) FROM device_command WHERE device_id = #{deviceId} AND state IN ('SENT','STARTED')")
    int activeCommands(String deviceId);

    @Select("SELECT COUNT(*) FROM device_command c JOIN task t ON t.id = c.task_id AND t.attempt = c.attempt"
            + " WHERE c.device_id = #{deviceId} AND c.state = 'FAILED' AND t.state = 'EXCEPTION'")
    int unresolvedFailures(String deviceId);

    @Insert("INSERT INTO device_command (id, task_id, device_id, attempt, state)"
            + " VALUES (#{id}, #{taskId}, #{deviceId}, #{attempt}, #{state})")
    void insertCommand(Command command);

    @Update("UPDATE device_command SET state = #{state} WHERE id = #{id}")
    void setCommandState(@Param("id") String id, @Param("state") CommandState state);

    @Select("SELECT * FROM command_event WHERE command_id = #{commandId} AND event_key = #{eventKey}")
    CommandEvent event(@Param("commandId") String commandId, @Param("eventKey") String eventKey);

    @Select("SELECT * FROM command_event WHERE command_id = #{commandId} ORDER BY created_at, event_key")
    List<CommandEvent> events(String commandId);

    @Insert("INSERT INTO command_event (command_id, event_key, event_type, observed_quantity, detail)"
            + " VALUES (#{commandId}, #{eventKey}, #{eventType}, #{observedQuantity}, #{detail})")
    void insertEvent(CommandEvent event);

    @Insert("INSERT INTO operation_audit (id, warehouse_id, document_id, task_id, action, detail)"
            + " VALUES (#{id}, #{warehouseId}, #{documentId}, #{taskId}, #{action}, #{detail})")
    void insertAudit(Audit audit);

    @Select("SELECT * FROM operation_audit WHERE document_id = #{documentId} ORDER BY created_at, id")
    List<Audit> audits(String documentId);
}
