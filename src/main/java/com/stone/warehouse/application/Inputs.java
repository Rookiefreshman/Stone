package com.stone.warehouse.application;

import java.time.LocalDate;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.*;

import static com.stone.warehouse.domain.WarehouseModel.*;

public final class Inputs {
    private Inputs() {}

    public static class MasterInput {
        @NotBlank @Pattern(regexp = "[A-Za-z0-9._-]{1,40}")
        public String code;
        @NotBlank @Size(max = 100)
        public String name;
        public LocationType locationType;
        public DeviceMode deviceMode;
    }

    public static class DocumentInput {
        @NotNull public Kind kind;
        @NotBlank @Pattern(regexp = "[A-Za-z0-9._:/-]{1,64}")
        public String externalRef;
        @Size(max = 36) public String sourceDocumentId;
        @Valid @NotEmpty @Size(max = 100)
        public List<@NotNull LineInput> lines;
    }

    public static class LineInput {
        @NotBlank @Size(max = 36) public String skuId;
        @Pattern(regexp = "[A-Za-z0-9._-]{1,40}") public String batchCode;
        public LocalDate expiresOn;
        @Size(max = 36) public String sourceId;
        @Size(max = 36) public String destinationId;
        @Min(0) @Max(1000000000L) public Long quantity;
    }

    public enum Action { APPROVE, RECEIVE, RELEASE, PACK, SHIP, ACCEPT_COUNT, CANCEL, UNPICK_CANCEL }

    public static class Reason {
        @NotBlank @Size(max = 240) public String reason;
    }

    public static class Reconciliation extends Reason {
        @NotNull public Boolean completed;
        @Min(0) @Max(MAX_STOCK_QUANTITY) public Long observedQuantity;
    }

    public static class WaveInput {
        @NotBlank @Pattern(regexp = "[A-Za-z0-9._-]{1,40}") public String code;
        @NotNull @Min(0) @Max(100) public Integer priority;
        @NotEmpty @Size(max = 50)
        public List<@NotBlank @Size(max = 36) String> documentIds;
    }

    public enum WaveAction { RELEASE, CANCEL }

    public static class DispatchInput {
        @NotBlank @Size(max = 36) public String deviceId;
    }

    public static class DeviceStateInput {
        @NotNull public DeviceState state;
    }

    public static class EventInput {
        @NotBlank @Pattern(regexp = "[A-Za-z0-9._:/-]{1,64}") public String eventKey;
        @NotNull public CommandState eventType;
        @Min(0) @Max(MAX_STOCK_QUANTITY) public Long observedQuantity;
        @Size(max = 240) public String detail;
    }
}
