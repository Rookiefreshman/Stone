package com.stone.wms.interfaces.controller;

import com.stone.wms.application.dto.DispatchDiagnosticDto;
import com.stone.wms.application.service.DispatchDiagnosticService;
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
import java.util.regex.Pattern;

/** Disabled by default. Development-only service credential and warehouse scope. */
@RestController
@ConditionalOnProperty(name="stone.agent.dispatch-query.enabled",havingValue="true")
@RequestMapping("/api/wes/warehouses/{warehouseId}/dispatch-diagnostic")
public class DispatchDiagnosticController {
    private static final Pattern DEVICE=Pattern.compile("[A-Za-z0-9._:/-]{1,64}");
    private final DispatchDiagnosticService service;
    private final byte[] expected;
    private final Set<Long> warehouses=new HashSet<>();
    public DispatchDiagnosticController(DispatchDiagnosticService service,
        @Value("${stone.agent.dispatch-query.token:}") String token,
        @Value("${stone.agent.dispatch-query.warehouses:}") String allowed) {
        if(token.length()<24 || !token.equals(token.trim())) throw new IllegalArgumentException("configure dispatch query token");
        this.service=service; expected=("Bearer "+token).getBytes(StandardCharsets.UTF_8);
        for(String item:allowed.split(",")) {
            long id=Long.parseLong(item.trim());
            if(id<=0) throw new IllegalArgumentException("invalid warehouse");
            warehouses.add(id);
        }
    }
    @GetMapping
    public ResponseEntity<ApiResponse<DispatchDiagnosticDto>> query(
        @PathVariable("warehouseId") long warehouseId,@RequestParam("deviceCode") String deviceCode,
        @RequestHeader(value="Authorization",required=false) String auth) {
        if(auth==null || !MessageDigest.isEqual(expected,auth.getBytes(StandardCharsets.UTF_8))) return error(HttpStatus.UNAUTHORIZED,"Unauthorized");
        if(!warehouses.contains(warehouseId)) return error(HttpStatus.FORBIDDEN,"Forbidden");
        if(deviceCode==null || !DEVICE.matcher(deviceCode).matches()) return error(HttpStatus.BAD_REQUEST,"Invalid device code");
        try {
            DispatchDiagnosticDto result=service.query(warehouseId,deviceCode);
            if(result==null) return error(HttpStatus.NOT_FOUND,"DISPATCH_SCOPE_NOT_FOUND");
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch(IllegalArgumentException e) {
            return error(HttpStatus.UNPROCESSABLE_ENTITY,"DISPATCH_SNAPSHOT_UNAVAILABLE");
        }
    }
    private ResponseEntity<ApiResponse<DispatchDiagnosticDto>> error(HttpStatus status,String message) {
        return ResponseEntity.status(status).body(ApiResponse.<DispatchDiagnosticDto>failure(message));
    }
}
