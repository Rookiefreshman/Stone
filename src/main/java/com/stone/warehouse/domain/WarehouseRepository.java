package com.stone.warehouse.domain;

import java.time.LocalDate;
import java.util.List;

import static com.stone.warehouse.domain.WarehouseModel.*;

public interface WarehouseRepository {
    List<Master> warehouses();
    Master lockWarehouse(String id);
    void insertWarehouse(Master master);
    List<Master> skus(String warehouseId);
    Master sku(String id);
    void insertSku(Master master);
    List<Master> locations(String warehouseId);
    Master location(String id);
    void insertLocation(Master master);
    List<Master> devices(String warehouseId);
    Master device(String id);
    void insertDevice(Master master);
    void setDeviceState(String id, DeviceState state);

    Lot lot(String skuId, String batchCode);
    Lot lotById(String id);
    void insertLot(Lot lot);
    List<Stock> stocks(String warehouseId, int limit, int offset);
    Stock stock(String locationId, String lotId);
    List<Stock> candidates(String skuId, String lotId, String sourceId, LocalDate today);
    void insertStock(Stock stock);
    void updateStock(Stock stock);
    void insertLedger(Ledger ledger);
    List<Ledger> ledger(String warehouseId, int limit, int offset);

    Document document(String id);
    Document documentByRef(String warehouseId, Kind kind, String externalRef);
    List<Document> documents(String warehouseId, int limit, int offset);
    void insertDocument(Document document);
    void setDocumentState(String id, DocumentState state);
    void insertLine(Line line);
    List<Line> lines(String documentId);
    long shipped(String documentId, String lotId);
    long returning(String sourceDocumentId, String lotId);

    Wave wave(String id);
    Wave activeWave(String documentId);
    List<Wave> waves(String warehouseId, int limit, int offset);
    void insertWave(Wave wave);
    void addWaveDocument(String waveId, String documentId, int sequenceNo);
    List<Document> waveDocuments(String waveId);
    void updateWave(Wave wave);

    Task task(String id);
    List<Task> tasks(String documentId);
    List<Task> queue(String warehouseId, int limit, int offset);
    void insertTask(Task task);
    void updateTask(Task task);
    int activeCount(String warehouseId, String locationId, String lotId);
    int activeWork(String warehouseId, String locationId, String lotId);

    Command command(String id);
    List<Command> commands(String taskId);
    int activeCommands(String deviceId);
    int unresolvedFailures(String deviceId);
    void insertCommand(Command command);
    void setCommandState(String id, CommandState state);
    CommandEvent event(String commandId, String eventKey);
    List<CommandEvent> events(String commandId);
    void insertEvent(CommandEvent event);
    void insertAudit(Audit audit);
    List<Audit> audits(String documentId);
}
