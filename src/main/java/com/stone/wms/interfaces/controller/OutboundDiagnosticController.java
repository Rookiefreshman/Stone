package com.stone.wms.interfaces.controller;

import com.stone.wms.application.dto.OutboundDiagnosticDto;
import com.stone.wms.application.service.OutboundDiagnosticService;
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

/** Development-only service credentials. Disabled unless explicitly configured. */
@RestController
@ConditionalOnProperty(name="stone.agent.outbound-query.enabled",havingValue="true")
@RequestMapping("/api/wms/warehouses/{warehouseId}/outbound-orders/{orderId}/allocation-diagnostic")
public class OutboundDiagnosticController {
    private final OutboundDiagnosticService service;
    private final byte[] expected;
    private final Set<Long> warehouses=new HashSet<>();
    public OutboundDiagnosticController(OutboundDiagnosticService service,
        @Value("${stone.agent.outbound-query.token:}") String token,
        @Value("${stone.agent.outbound-query.warehouses:}") String allowed) {
        if(token.length()<24 || !token.equals(token.trim())) throw new IllegalArgumentException("configure outbound query token");
        this.service=service; expected=("Bearer "+token).getBytes(StandardCharsets.UTF_8);
        for(String item:allowed.split(",")) {
            long id=Long.parseLong(item.trim());
            if(id<=0) throw new IllegalArgumentException("invalid warehouse");
            warehouses.add(id);
        }
    }
    @GetMapping
    public ResponseEntity<ApiResponse<OutboundDiagnosticDto>> query(
        @PathVariable("warehouseId") long warehouseId,@PathVariable("orderId") long orderId,
        @RequestHeader(value="Authorization",required=false) String auth) {
        if(auth==null || !MessageDigest.isEqual(expected,auth.getBytes(StandardCharsets.UTF_8))) return error(HttpStatus.UNAUTHORIZED,"Unauthorized");
        if(!warehouses.contains(warehouseId)) return error(HttpStatus.FORBIDDEN,"Forbidden");
        if(orderId<=0) return error(HttpStatus.BAD_REQUEST,"Invalid order ID");
        try {
            OutboundDiagnosticDto result=service.query(warehouseId,orderId);
            if(result==null) return error(HttpStatus.NOT_FOUND,"ORDER_NOT_FOUND");
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch(IllegalArgumentException e) {
            return error(HttpStatus.UNPROCESSABLE_ENTITY,"ORDER_SNAPSHOT_UNAVAILABLE");
        }
    }
    private ResponseEntity<ApiResponse<OutboundDiagnosticDto>> error(HttpStatus status,String message) {
        return ResponseEntity.status(status).body(ApiResponse.<OutboundDiagnosticDto>failure(message));
    }
}
