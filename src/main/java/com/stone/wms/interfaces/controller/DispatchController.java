package com.stone.wms.interfaces.controller;

import com.stone.wms.application.dto.DispatchTaskDto;
import com.stone.wms.application.service.DispatchApplicationService;
import com.stone.wms.domain.exception.DispatchException;
import com.stone.wms.domain.model.dispatch.DispatchTask.State;
import com.stone.wms.interfaces.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.*;
import java.util.List;

@RestController
@RequestMapping("/api/wes/warehouses/{warehouseId}")
public class DispatchController {
    private static final String KEY = "[A-Za-z0-9._:/-]{1,64}";

    private final DispatchApplicationService service;

    public DispatchController(DispatchApplicationService service) {
        this.service = service;
    }

    @InitBinder
    void useDirectFieldValidation(WebDataBinder binder) {
        binder.initDirectFieldAccess();
    }

    @PostMapping("/tasks")
    ApiResponse<DispatchTaskDto> enqueue(@PathVariable long warehouseId, @Valid @RequestBody Enqueue request) {
        return ApiResponse.success(service.enqueue(warehouseId, request.taskKey, request.priority));
    }

    @PostMapping("/claims")
    ApiResponse<List<DispatchTaskDto>> claim(@PathVariable long warehouseId, @Valid @RequestBody Claim request) {
        return ApiResponse.success(service.claim(warehouseId, request.deviceCode, request.claimKey, request.limit));
    }

    @PostMapping("/tasks/{taskId}/events")
    ApiResponse<DispatchTaskDto> report(@PathVariable long warehouseId, @PathVariable long taskId,
                                       @Valid @RequestBody Feedback request) {
        return ApiResponse.success(service.report(warehouseId, taskId, request.deviceCode, request.claimKey,
                request.eventKey, request.state, Boolean.TRUE.equals(request.reconciled), request.reason));
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

    public static class Feedback {
        @NotNull @Pattern(regexp = KEY) public String deviceCode;
        @NotNull @Pattern(regexp = KEY) public String claimKey;
        @NotNull @Pattern(regexp = KEY) public String eventKey;
        @NotNull public State state;
        public Boolean reconciled;
        @Size(max = 240) public String reason;
    }
}
