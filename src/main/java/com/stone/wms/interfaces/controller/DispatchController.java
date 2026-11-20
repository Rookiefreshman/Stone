package com.stone.wms.interfaces.controller;

import com.stone.wms.application.dto.DispatchTaskDto;
import com.stone.wms.application.dto.StockWorkDetailDto;
import com.stone.wms.application.service.DispatchApplicationService;
import com.stone.wms.domain.exception.DispatchException;
import com.stone.wms.domain.exception.DomainException;
import com.stone.wms.domain.model.inventory.StockWork;
import com.stone.wms.domain.model.dispatch.DispatchTask.State;
import com.stone.wms.interfaces.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.*;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api")
public class DispatchController {
    private static final String WES = "/wes/warehouses/{warehouseId}";
    private static final String WMS = "/wms/warehouses/{warehouseId}";
    private static final String LOT = "[A-Za-z0-9._-]{1,64}";
    private static final String KEY = "[A-Za-z0-9._:/-]{1,64}";

    private final DispatchApplicationService service;

    public DispatchController(DispatchApplicationService service) {
        this.service = service;
    }

    @InitBinder
    void useDirectFieldValidation(WebDataBinder binder) {
        binder.initDirectFieldAccess();
    }

    @PostMapping(WES + "/tasks")
    ApiResponse<DispatchTaskDto> enqueue(@PathVariable long warehouseId, @Valid @RequestBody Enqueue request) {
        return ApiResponse.success(service.enqueue(warehouseId, request.taskKey, request.priority));
    }

    @PostMapping(WES + "/claims")
    ApiResponse<List<DispatchTaskDto>> claim(@PathVariable long warehouseId, @Valid @RequestBody Claim request) {
        return ApiResponse.success(service.claim(warehouseId, request.deviceCode, request.claimKey, request.limit));
    }

    @PostMapping(WES + "/tasks/{taskId}/events")
    ApiResponse<DispatchTaskDto> report(@PathVariable long warehouseId, @PathVariable long taskId,
                                       @Valid @RequestBody Feedback request) {
        return ApiResponse.success(service.report(warehouseId, taskId, request.deviceCode, request.claimKey,
                request.eventKey, request.state, Boolean.TRUE.equals(request.reconciled), request.reason, request.completedQty));
    }

    @PostMapping(WMS + "/receipt-lines")
    ApiResponse<Void> registerReceipt(@PathVariable long warehouseId, @Valid @RequestBody Receipt request) {
        service.registerReceipt(warehouseId, request.receiptKey, request.skuId, request.lotNo.toUpperCase(Locale.ROOT),
                request.quantity);
        return ApiResponse.success(null);
    }

    @PostMapping(WMS + "/stock-work")
    ApiResponse<DispatchTaskDto> submitStockWork(@PathVariable long warehouseId, @Valid @RequestBody Work request) {
        StockWork work = new StockWork(request.type, request.receiptKey, request.skuId,
                request.lotNo.toUpperCase(Locale.ROOT), request.sourceId, request.destinationId, request.quantity);
        return ApiResponse.success(service.submitStockWork(warehouseId, request.taskKey, request.priority, work));
    }

    @GetMapping(WMS + "/stock-work/{taskId}")
    ApiResponse<StockWorkDetailDto> stockWork(@PathVariable long warehouseId, @PathVariable long taskId) {
        return ApiResponse.success(service.stockWorkDetail(warehouseId, taskId));
    }

    @PostMapping(WMS + "/stock-work/{taskId}/cancel")
    ApiResponse<DispatchTaskDto> cancel(@PathVariable long warehouseId, @PathVariable long taskId) {
        return ApiResponse.success(service.cancelStockWork(warehouseId, taskId));
    }

    @PostMapping(WMS + "/locations/{locationId}/freeze")
    ApiResponse<Boolean> freeze(@PathVariable long warehouseId, @PathVariable long locationId,
                                @Valid @RequestBody Freeze request) {
        return ApiResponse.success(service.freezeLocation(warehouseId, locationId, request.freezeKey, request.reason));
    }

    @PostMapping(WMS + "/locations/{locationId}/unfreeze")
    ApiResponse<Boolean> unfreeze(@PathVariable long warehouseId, @PathVariable long locationId,
                                  @Valid @RequestBody FreezeKey request) {
        return ApiResponse.success(service.unfreezeLocation(warehouseId, locationId, request.freezeKey));
    }

    @ExceptionHandler(DomainException.class)
    ResponseEntity<ApiResponse<Void>> stockConflict(DomainException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.failure(exception.getMessage()));
    }

    @ExceptionHandler(DispatchException.class)
    ResponseEntity<ApiResponse<Void>> dispatchError(DispatchException exception) {
        HttpStatus status;
        switch (exception.getCode()) {
            case NOT_FOUND: status = HttpStatus.NOT_FOUND; break;
            case QUEUE_FULL: status = HttpStatus.TOO_MANY_REQUESTS; break;
            default: status = HttpStatus.CONFLICT;
        }
        return ResponseEntity.status(status).body(ApiResponse.failure(exception.getMessage()));
    }

    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<Void>> invalid() {
        return ResponseEntity.badRequest().body(ApiResponse.failure("请求参数不合法"));
    }

    public static class Enqueue {
        @NotNull @Pattern(regexp = KEY) public String taskKey;
        @NotNull @Min(0) @Max(100) public Integer priority;
    }

    public static class Claim {
        @NotNull @Pattern(regexp = KEY) public String deviceCode;
        @NotNull @Pattern(regexp = KEY) public String claimKey;
        @NotNull @Min(1) @Max(100) public Integer limit;
    }

    public static class Receipt {
        @NotNull @Pattern(regexp = KEY) public String receiptKey;
        @NotNull @Positive public Long skuId;
        @NotNull @Pattern(regexp = LOT) public String lotNo;
        @NotNull @Positive public Integer quantity;
    }

    public static class Work extends Enqueue {
        @NotNull public StockWork.Type type;
        @Pattern(regexp = KEY) public String receiptKey;
        @NotNull @Positive public Long skuId;
        @NotNull @Pattern(regexp = LOT) public String lotNo;
        @Positive public Long sourceId;
        @NotNull @Positive public Long destinationId;
        @NotNull @Positive public Integer quantity;
    }

    public static class FreezeKey {
        @NotNull @Pattern(regexp = KEY) public String freezeKey;
    }

    public static class Freeze extends FreezeKey {
        @NotBlank @Size(max = 240) public String reason;
    }

    public static class Feedback {
        @NotNull @Pattern(regexp = KEY) public String deviceCode;
        @NotNull @Pattern(regexp = KEY) public String claimKey;
        @NotNull @Pattern(regexp = KEY) public String eventKey;
        @NotNull public State state;
        public Boolean reconciled;
        @Min(0) public Integer completedQty;
        @Size(max = 240) public String reason;
    }
}
