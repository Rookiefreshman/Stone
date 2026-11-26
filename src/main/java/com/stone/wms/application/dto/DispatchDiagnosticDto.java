package com.stone.wms.application.dto;

import com.stone.wms.domain.repository.DispatchDiagnosticRepository.Row;

public final class DispatchDiagnosticDto {
    public final long warehouseId;
    public final String deviceCode;
    public final long maxReady,maxActive,maxBatch,deviceMaxActive;
    public final long ready,inFlight,unknown,deviceInFlight,deviceUnknown;
    public DispatchDiagnosticDto(Row r) {
        warehouseId=r.warehouseId; deviceCode=r.deviceCode;
        maxReady=r.maxReady; maxActive=r.maxActive; maxBatch=r.maxBatch; deviceMaxActive=r.deviceMaxActive;
        ready=r.ready; inFlight=r.inFlight; unknown=r.unknown;
        deviceInFlight=r.deviceInFlight; deviceUnknown=r.deviceUnknown;
    }
}
