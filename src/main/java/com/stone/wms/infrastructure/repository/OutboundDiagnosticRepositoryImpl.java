package com.stone.wms.infrastructure.repository;

import com.stone.wms.domain.repository.OutboundDiagnosticRepository;
import com.stone.wms.infrastructure.mapper.OutboundDiagnosticMapper;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public class OutboundDiagnosticRepositoryImpl implements OutboundDiagnosticRepository {
    private final OutboundDiagnosticMapper mapper;
    public OutboundDiagnosticRepositoryImpl(OutboundDiagnosticMapper mapper) { this.mapper=mapper; }
    @Override public List<Row> snapshot(long warehouseId,long orderId) { return mapper.snapshot(warehouseId,orderId); }
}
