package com.stone.warehouse.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stone.warehouse.domain.WarehouseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import static com.stone.warehouse.application.Inputs.*;
import static com.stone.warehouse.domain.WarehouseModel.*;
import static com.stone.warehouse.domain.WarehouseProblem.*;

@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class DocumentService {
    private final WarehouseRepository repository;
    private final InventoryService inventory;
    private final ObjectMapper json;

    public DocumentService(WarehouseRepository repository, InventoryService inventory, ObjectMapper json) {
        this.repository = repository;
        this.inventory = inventory;
        this.json = json;
    }

    public Document create(String warehouseId, DocumentInput input) {
        found(repository.lockWarehouse(warehouseId), "仓库");
        String ref = input.externalRef.toUpperCase(Locale.ROOT);
        String hash = fingerprint(input);
        Document existing = repository.documentByRef(warehouseId, input.kind, ref);
        if (existing != null) {
            require(existing.requestHash.equals(hash), "REQUEST_CONFLICT", "外部单号已用于不同的请求");
            return existing;
        }
        require((input.kind == Kind.RETURN) == (input.sourceDocumentId != null),
                "INVALID_DOCUMENT", "只有退货单必须关联已发运的原出库单");
        if (input.kind == Kind.RETURN) {
            Document original = found(repository.document(input.sourceDocumentId), "原出库单");
            require(warehouseId.equals(original.warehouseId) && original.kind == Kind.OUTBOUND
                            && original.state == DocumentState.COMPLETED,
                    "INVALID_RETURN", "原单必须是同仓已发运的出库单");
        }
        Document document = new Document();
        document.id = UUID.randomUUID().toString();
        document.warehouseId = warehouseId;
        document.kind = input.kind;
        document.externalRef = ref;
        document.sourceDocumentId = input.sourceDocumentId;
        document.state = DocumentState.DRAFT;
        document.requestHash = hash;
        repository.insertDocument(document);
        int number = 0;
        for (LineInput inputLine : input.lines) {
            Line line = line(document, inputLine, ++number);
            if (document.kind == Kind.RETURN) {
                long remaining = repository.shipped(document.sourceDocumentId, line.lotId)
                        - repository.returning(document.sourceDocumentId, line.lotId);
                require(line.quantity <= remaining, "RETURN_EXCEEDS_SHIPMENT", "该批次退货总量超出原出库数量");
            }
            repository.insertLine(line);
        }
        return repository.document(document.id);
    }

    private String fingerprint(DocumentInput input) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(json.writeValueAsBytes(input));
            StringBuilder hex = new StringBuilder();
            for (byte value : bytes) {
                hex.append(Character.forDigit((value & 0xff) >>> 4, 16));
                hex.append(Character.forDigit(value & 0xf, 16));
            }
            return hex.toString();
        } catch (JsonProcessingException | NoSuchAlgorithmException exception) {
            throw new IllegalStateException("无法计算请求摘要", exception);
        }
    }

    private Line line(Document document, LineInput input, int index) {
        Kind kind = document.kind;
        Master sku = found(repository.sku(input.skuId), "商品");
        require(document.warehouseId.equals(sku.warehouseId), "WRONG_WAREHOUSE", "商品不属于单据仓库");
        validateLocations(document, input);
        require(kind == Kind.COUNT ? input.quantity == null : input.quantity != null && input.quantity > 0,
                "INVALID_QUANTITY", "非盘点单的数量须为正数，盘点计划不填写数量");
        require(kind == Kind.OUTBOUND || input.batchCode != null, "BATCH_REQUIRED", "该单据需要指定批次");
        require(kind == Kind.INBOUND || input.expiresOn == null, "INVALID_EXPIRY", "有效期由首次入库登记");
        Lot lot = resolveLot(kind, sku.id, input);
        Line line = new Line();
        line.id = UUID.randomUUID().toString();
        line.documentId = document.id;
        line.lineNo = index;
        line.skuId = sku.id;
        line.lotId = lot == null ? null : lot.id;
        line.sourceId = input.sourceId;
        line.destinationId = input.destinationId;
        line.quantity = input.quantity == null ? 0 : input.quantity;
        return line;
    }

    private Lot resolveLot(Kind kind, String skuId, LineInput input) {
        if (input.batchCode == null) {
            return null;
        }
        String code = input.batchCode.toUpperCase(Locale.ROOT);
        Lot lot = repository.lot(skuId, code);
        if (lot == null && kind == Kind.INBOUND) {
            lot = new Lot();
            lot.id = UUID.randomUUID().toString();
            lot.skuId = skuId;
            lot.batchCode = code;
            lot.expiresOn = input.expiresOn;
            repository.insertLot(lot);
        }
        found(lot, "商品批次");
        require(input.expiresOn == null || Objects.equals(input.expiresOn, lot.expiresOn),
                "INVALID_EXPIRY", "批次的有效期与已有档案不一致");
        return lot;
    }

    private void validateLocations(Document document, LineInput input) {
        Kind kind = document.kind;
        boolean sourceRequired = kind.movement() || kind == Kind.COUNT;
        boolean destinationRequired = kind.movement() || kind.receipt();
        require((sourceRequired ? input.sourceId != null : input.sourceId == null || kind == Kind.OUTBOUND)
                        && destinationRequired == (input.destinationId != null),
                "INVALID_LOCATION", "单据的源库位或目标库位组合不正确");
        Master source = location(document, input.sourceId);
        Master destination = location(document, input.destinationId);
        if (source != null && destination != null) {
            require(!source.id.equals(destination.id), "INVALID_LOCATION", "源库位与目标库位须不同");
        }
        if (source != null && kind != Kind.COUNT) {
            require(source.locationType != LocationType.QUARANTINE,
                    "INVALID_LOCATION", "隔离库存不可直接分配");
        }
        if (kind == Kind.REPLENISH) {
            require(source.locationType == LocationType.STORAGE && destination.locationType == LocationType.PICK_FACE,
                    "INVALID_LOCATION", "补货须从存储位移至拣选位");
        }
        if (kind == Kind.RETURN) {
            require(destination.locationType == LocationType.QUARANTINE, "INVALID_LOCATION", "退货必须进入隔离位");
        }
    }

    private Master location(Document document, String id) {
        if (id == null) {
            return null;
        }
        Master location = found(repository.location(id), "库位");
        require(document.warehouseId.equals(location.warehouseId), "WRONG_WAREHOUSE", "库位不属于单据仓库");
        return location;
    }

    public Document act(String documentId, Action action, String reason) {
        Document document = locked(documentId);
        switch (action) {
            case APPROVE:
                expect(document, DocumentState.DRAFT);
                repository.setDocumentState(document.id, DocumentState.APPROVED);
                break;
            case RECEIVE:
                expect(document, DocumentState.APPROVED);
                require(document.kind.receipt(), "INVALID_ACTION", "仅入库和退货单可收货");
                release(document);
                break;
            case RELEASE:
                expect(document, DocumentState.APPROVED);
                require(!document.kind.receipt(), "INVALID_ACTION", "入库和退货单需要先收货");
                require(repository.activeWave(document.id) == null, "WAVE_ASSIGNED", "请通过所属波次释放任务");
                release(document);
                break;
            case PACK:
                expect(document, DocumentState.PICKED);
                repository.setDocumentState(document.id, DocumentState.PACKED);
                break;
            case SHIP:
                expect(document, DocumentState.PACKED);
                for (Task task : repository.tasks(document.id)) {
                    inventory.ensureEligibleBatch(task);
                    inventory.post(task, task.sourceId, -task.quantity, -task.quantity, "SHIP");
                }
                repository.setDocumentState(document.id, DocumentState.COMPLETED);
                break;
            case ACCEPT_COUNT:
                expect(document, DocumentState.COUNTED);
                for (Task task : repository.tasks(document.id)) {
                    long difference = task.observedQuantity - task.quantity;
                    if (difference != 0) {
                        inventory.post(task, task.sourceId, difference, 0, "COUNT_ADJUST");
                    }
                }
                repository.setDocumentState(document.id, DocumentState.COMPLETED);
                break;
            case CANCEL:
                Wave wave = repository.activeWave(document.id);
                require(wave == null || wave.state != WaveState.DRAFT, "WAVE_ASSIGNED", "请先取消草稿波次");
                cancel(document, false);
                break;
            case UNPICK_CANCEL:
                cancel(document, true);
                break;
            default: throw new AssertionError(action);
        }
        audit(document, null, action.name(), reason);
        return repository.document(document.id);
    }

    void releaseWaveDocument(Document document, String reason) {
        expect(document, DocumentState.APPROVED);
        release(document);
        audit(document, null, "WAVE_RELEASE", reason);
    }

    private void release(Document document) {
        repository.setDocumentState(document.id, DocumentState.EXECUTING);
        for (Line line : repository.lines(document.id)) {
            if (document.kind.receipt()) {
                inventory.ensureUnfrozen(document.warehouseId, line.destinationId, line.lotId);
                inventory.createTask(document, line, line.lotId, null, line.quantity);
            } else if (document.kind == Kind.COUNT) {
                require(repository.activeWork(document.warehouseId, line.sourceId, line.lotId) == 0,
                        "COUNT_SCOPE_BUSY", "盘点范围存在活动任务或重复的盘点行");
                Stock stock = repository.stock(line.sourceId, line.lotId);
                inventory.createTask(document, line, line.lotId, line.sourceId, stock == null ? 0 : stock.onHand);
            } else {
                inventory.reserve(document, line);
            }
        }
    }

    private void cancel(Document document, boolean unpicked) {
        if (unpicked) {
            require(document.kind == Kind.OUTBOUND, "INVALID_ACTION", "仅出库单允许确认退拣后取消");
        }
        require(document.state == DocumentState.DRAFT || document.state == DocumentState.APPROVED
                        || document.state == DocumentState.EXECUTING
                        || (unpicked && (document.state == DocumentState.PICKED || document.state == DocumentState.PACKED)),
                "INVALID_STATE", "当前单据状态不可取消");
        List<Task> tasks = repository.tasks(document.id);
        if (unpicked) {
            require(tasks.stream().anyMatch(task -> task.state == TaskState.DONE),
                    "INVALID_ACTION", "没有已确认完成的拣选可退拣");
        }
        for (Task task : tasks) {
            require(task.state == TaskState.READY || (unpicked && task.state == TaskState.DONE), "ACTIVE_WORK", "已执行或执行结果待确认的单据不可取消");
        }
        for (Task task : tasks) {
            if (document.kind == Kind.OUTBOUND || document.kind.movement()) {
                inventory.post(task, task.sourceId, 0, -task.quantity, "RELEASE");
            }
            task.state = TaskState.CANCELLED;
            repository.updateTask(task);
        }
        repository.setDocumentState(document.id, DocumentState.CANCELLED);
    }

    Document locked(String id) {
        Document document = found(repository.document(id), "单据");
        found(repository.lockWarehouse(document.warehouseId), "仓库");
        return found(repository.document(id), "单据");
    }

    void audit(Document document, Task task, String action, String detail) {
        Audit audit = new Audit();
        audit.id = UUID.randomUUID().toString();
        audit.warehouseId = document.warehouseId;
        audit.documentId = document.id;
        audit.taskId = task == null ? null : task.id;
        audit.action = action;
        audit.detail = detail;
        repository.insertAudit(audit);
    }

    private void expect(Document document, DocumentState state) {
        require(document.state == state, "INVALID_STATE", "此动作要求单据状态为 " + state);
    }
}
