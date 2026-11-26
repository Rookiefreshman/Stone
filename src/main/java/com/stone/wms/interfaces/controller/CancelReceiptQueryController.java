package com.stone.wms.interfaces.controller;

import com.stone.wms.application.service.CancelOperationService;
import com.stone.wms.domain.repository.CancelReceiptRepository.Receipt;
import com.stone.wms.interfaces.response.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Set;

/** Dedicated read-only development credential. Default off. Never calls submit. */
@RestController
@ConditionalOnProperty(name="stone.agent.cancel-receipt-query.enabled",havingValue="true")
@RequestMapping("/api/wms/warehouses/{warehouseId}/cancel-operation-receipts")
public class CancelReceiptQueryController {
    private final CancelOperationService service;
    private final byte[] expected;
    private final Set<Long> warehouses=new HashSet<>();
    public CancelReceiptQueryController(CancelOperationService service,
        @Value("${stone.agent.cancel-receipt-query.token:}") String token,
        @Value("${stone.agent.cancel-receipt-query.warehouses:}") String allowed) {
        if(token.length()<24 || !token.equals(token.trim()))throw new IllegalArgumentException("configure receipt reader credential");
        this.service=service;expected=("Bearer "+token).getBytes(StandardCharsets.UTF_8);
        for(String item:allowed.split(",")){long id=Long.parseLong(item.trim());if(id<=0)throw new IllegalArgumentException("invalid warehouse");warehouses.add(id);}
    }
    @GetMapping("/{operationId}")
    public ResponseEntity<ApiResponse<Receipt>> query(@PathVariable("warehouseId") long warehouse,
        @PathVariable("operationId") String operation,@RequestHeader(value="Authorization",required=false) String auth) {
        if(auth==null || !MessageDigest.isEqual(expected,auth.getBytes(StandardCharsets.UTF_8)))return error(HttpStatus.UNAUTHORIZED,"Unauthorized");
        if(!warehouses.contains(warehouse))return error(HttpStatus.FORBIDDEN,"Forbidden");
        if(operation==null || !operation.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))
            return error(HttpStatus.BAD_REQUEST,"Invalid operation ID");
        Receipt result=service.query(warehouse,operation);
        if(result==null)return error(HttpStatus.NOT_FOUND,"OPERATION_RECEIPT_NOT_FOUND");
        return ResponseEntity.ok(ApiResponse.success(result));
    }
    private ResponseEntity<ApiResponse<Receipt>> error(HttpStatus status,String message){return ResponseEntity.status(status).body(ApiResponse.<Receipt>failure(message));}
}
