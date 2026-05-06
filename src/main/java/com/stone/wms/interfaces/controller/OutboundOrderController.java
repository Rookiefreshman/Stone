package com.stone.wms.interfaces.controller;

import com.stone.wms.application.dto.AllocateOutboundCommand;
import com.stone.wms.application.dto.OutboundOrderDto;
import com.stone.wms.application.service.OutboundAllocationApplicationService;
import com.stone.wms.interfaces.request.AllocateOutboundRequest;
import com.stone.wms.interfaces.response.ApiResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 出库单接口层只做参数接收和应用服务调用，不直接访问 Mapper 或 Entity。
 */
@RestController
@RequestMapping("/api/wms/outbound-orders")
public class OutboundOrderController {
    private final OutboundAllocationApplicationService outboundAllocationApplicationService;

    public OutboundOrderController(OutboundAllocationApplicationService outboundAllocationApplicationService) {
        this.outboundAllocationApplicationService = outboundAllocationApplicationService;
    }

    @PostMapping("/{orderId}/allocate")
    public ApiResponse<OutboundOrderDto> allocate(@PathVariable("orderId") Long orderId,
                                                  @Valid @RequestBody AllocateOutboundRequest request) {
        AllocateOutboundCommand command = new AllocateOutboundCommand(orderId, request.getIdempotencyKey());
        return ApiResponse.success(outboundAllocationApplicationService.allocate(command));
    }
}
