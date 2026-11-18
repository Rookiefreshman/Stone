package com.stone.warehouse.domain;

public final class WarehouseModel {
    private WarehouseModel() {}

    public enum Kind {
        INBOUND, OUTBOUND, RETURN, TRANSFER, REPLENISH, COUNT;

        public boolean receipt() { return this == INBOUND || this == RETURN; }
        public boolean movement() { return this == TRANSFER || this == REPLENISH; }
    }

    public enum DocumentState {
        DRAFT, APPROVED, EXECUTING, PICKED, PACKED, COUNTED, COMPLETED, CANCELLED
    }

    public enum WaveState { DRAFT, RELEASED, CANCELLED }

    public static class Wave {
        public String id;
        public String warehouseId;
        public String code;
        public int priority;
        public WaveState state;
        public String reason;
        public java.time.LocalDateTime createdAt;
    }

    public enum TaskState { READY, RUNNING, DONE, EXCEPTION, CANCELLED }

    public enum LocationType { STORAGE, PICK_FACE, QUARANTINE }

    public enum DeviceMode { SIMULATOR, MANUAL }

    public enum DeviceState { ENABLED, DISABLED }

    public enum CommandState { SENT, STARTED, SUCCEEDED, FAILED }

    public static class Master {
        public String id;
        public String warehouseId;
        public String code;
        public String name;
        public LocationType locationType;
        public DeviceMode deviceMode;
        public DeviceState state;
    }

    public static class Lot {
        public String id;
        public String skuId;
        public String batchCode;
        public java.time.LocalDate expiresOn;
    }

    public static class Document {
        public String id;
        public String warehouseId;
        public Kind kind;
        public String externalRef;
        public String sourceDocumentId;
        public DocumentState state;
        public String requestHash;
        public java.time.LocalDateTime createdAt;
    }

    public static class Line {
        public String id;
        public String documentId;
        public int lineNo;
        public String skuId;
        public String lotId;
        public String batchCode;
        public String sourceId;
        public String destinationId;
        public long quantity;
    }

    public static class Task {
        public String id;
        public String warehouseId;
        public String documentId;
        public String lineId;
        public String lotId;
        public String batchCode;
        public String sourceId;
        public String destinationId;
        public long quantity;
        public Long observedQuantity;
        public TaskState state;
        public int attempt;
    }

    public static final long MAX_STOCK_QUANTITY = 1_000_000_000_000L;

    public static class Stock {
        public String locationId;
        public String locationCode;
        public LocationType locationType;
        public String lotId;
        public String skuId;
        public String skuCode;
        public String batchCode;
        public java.time.LocalDate expiresOn;
        public long onHand;
        public long reserved;

        public long available() { return onHand - reserved; }

        public void apply(long physical, long allocated) {
            onHand = Math.addExact(onHand, physical);
            reserved = Math.addExact(reserved, allocated);
            WarehouseProblem.require(reserved >= 0 && onHand >= reserved && onHand <= MAX_STOCK_QUANTITY, "STOCK_CONFLICT",
                    "库存变更后必须满足 0 ≤ 已预留 ≤ 在库 ≤ 1 万亿");
        }
    }

    public static class Command {
        public String id;
        public String taskId;
        public String deviceId;
        public int attempt;
        public CommandState state;
        public java.time.LocalDateTime createdAt;
    }

    public static class CommandEvent {
        public String commandId;
        public String eventKey;
        public CommandState eventType;
        public Long observedQuantity;
        public String detail;
    }

    public static class Ledger {
        public String id;
        public String warehouseId;
        public String taskId;
        public String locationId;
        public String lotId;
        public String reason;
        public String batchCode;
        public long deltaOnHand;
        public long deltaReserved;
        public java.time.LocalDateTime createdAt;
    }

    public static class Audit {
        public String id;
        public String warehouseId;
        public String documentId;
        public String taskId;
        public String action;
        public String detail;
        public java.time.LocalDateTime createdAt;
    }
}
