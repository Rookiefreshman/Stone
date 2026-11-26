package com.stone.wms.application.service;

import com.stone.wms.application.dto.DispatchDiagnosticDto;
import com.stone.wms.domain.repository.DispatchDiagnosticRepository;
import com.stone.wms.domain.repository.DispatchDiagnosticRepository.Row;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DispatchDiagnosticService {
    private final DispatchDiagnosticRepository repository;
    public DispatchDiagnosticService(DispatchDiagnosticRepository repository) { this.repository=repository; }
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public DispatchDiagnosticDto query(long warehouseId,String deviceCode) {
        Row r=repository.snapshot(warehouseId,deviceCode);
        if(r==null) return null;
        if(r.warehouseId!=warehouseId || !deviceCode.equals(r.deviceCode))
            throw new IllegalArgumentException("SCOPE_MISMATCH");
        // Existing claim service uses signed int capacities and active counts.
        long[] values={r.maxReady,r.maxActive,r.maxBatch,r.deviceMaxActive,r.ready,r.inFlight,r.unknown,r.deviceInFlight,r.deviceUnknown};
        for(long v:values) if(v<0 || v>Integer.MAX_VALUE) throw new IllegalArgumentException("COUNT_OUT_OF_RANGE");
        if(r.deviceInFlight>r.inFlight || r.deviceUnknown>r.unknown || r.inFlight+r.unknown>Integer.MAX_VALUE)
            throw new IllegalArgumentException("INVALID_COUNTS");
        return new DispatchDiagnosticDto(r);
    }
}
