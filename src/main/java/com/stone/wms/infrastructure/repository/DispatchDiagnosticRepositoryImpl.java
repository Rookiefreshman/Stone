package com.stone.wms.infrastructure.repository;

import com.stone.wms.domain.repository.DispatchDiagnosticRepository;
import com.stone.wms.infrastructure.mapper.DispatchDiagnosticMapper;
import org.springframework.stereotype.Repository;

@Repository
public class DispatchDiagnosticRepositoryImpl implements DispatchDiagnosticRepository {
    private final DispatchDiagnosticMapper mapper;
    public DispatchDiagnosticRepositoryImpl(DispatchDiagnosticMapper mapper) { this.mapper=mapper; }
    @Override public Row snapshot(long warehouseId,String deviceCode) { return mapper.snapshot(warehouseId,deviceCode); }
}
