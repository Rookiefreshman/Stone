package com.stone.wms.interfaces.controller;

import com.stone.wms.application.dto.DispatchHistoryDto;
import com.stone.wms.application.service.DispatchHistoryQueryService;
import com.stone.wms.domain.exception.DispatchException;
import com.stone.wms.interfaces.response.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Set;

/** Disabled by default. Dev-only service credential; replace with enterprise identity before rollout. */
@RestController
@ConditionalOnProperty(name="stone.agent.event-query.enabled",havingValue="true")
@RequestMapping("/api/wes/warehouses/{warehouseId}/tasks/{taskId}/events")
public class DispatchHistoryQueryController {
    private final DispatchHistoryQueryService service;
    private final byte[] expected;
    private final Set<Long> warehouses=new HashSet<>();
    public DispatchHistoryQueryController(DispatchHistoryQueryService service,
        @Value("${stone.agent.event-query.token:}") String token,
        @Value("${stone.agent.event-query.warehouses:}") String allowed) {
        if(token.length()<24 || token.trim().length()!=token.length()) throw new IllegalArgumentException("configure event query token");
        this.service=service; this.expected=("Bearer "+token).getBytes(StandardCharsets.UTF_8);
        for(String item:allowed.split(",")) {
            long id=Long.parseLong(item.trim());
            if(id<=0) throw new IllegalArgumentException("invalid warehouse");
            warehouses.add(id);
        }
        if(warehouses.isEmpty()) throw new IllegalArgumentException("configure warehouse scope");
    }
    @GetMapping
    public ResponseEntity<ApiResponse<DispatchHistoryDto>> query(@PathVariable("warehouseId") long warehouseId,
        @PathVariable("taskId") long taskId,@RequestHeader(value="Authorization",required=false) String auth) {
        if(auth==null || !MessageDigest.isEqual(expected,auth.getBytes(StandardCharsets.UTF_8)))
            return error(HttpStatus.UNAUTHORIZED,"Unauthorized");
        if(!warehouses.contains(warehouseId)) return error(HttpStatus.FORBIDDEN,"Forbidden");
        if(taskId<=0) return error(HttpStatus.BAD_REQUEST,"Invalid task ID");
        try { return ResponseEntity.ok(ApiResponse.success(service.query(warehouseId,taskId))); }
        catch(DispatchException e) {
            if(e.getCode()==DispatchException.Code.NOT_FOUND) return error(HttpStatus.NOT_FOUND,"库存作业不存在");
            throw e;
        }
    }
    private ResponseEntity<ApiResponse<DispatchHistoryDto>> error(HttpStatus status,String message) {
        return ResponseEntity.status(status).body(ApiResponse.<DispatchHistoryDto>failure(message));
    }
}
