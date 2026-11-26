package com.stone.wms.application.service;

import com.stone.wms.application.dto.OutboundDiagnosticDto;
import com.stone.wms.domain.repository.OutboundDiagnosticRepository;
import com.stone.wms.domain.repository.OutboundDiagnosticRepository.Row;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class OutboundDiagnosticService {
    private final OutboundDiagnosticRepository repository;
    public OutboundDiagnosticService(OutboundDiagnosticRepository repository) { this.repository=repository; }
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public OutboundDiagnosticDto query(long warehouseId,long orderId) {
        List<Row> rows=repository.snapshot(warehouseId,orderId);
        if(rows.isEmpty()) return null;
        if(rows.size()>200) throw new IllegalArgumentException("ORDER_LINE_LIMIT");
        Set<Long> ids=new HashSet<>();
        Row first=rows.get(0);
        for(Row row:rows) {
            if(row.warehouseId!=warehouseId || row.orderId!=orderId || row.lineId==null
                || !ids.add(row.lineId) || !Objects.equals(row.orderNo,first.orderNo)
                || !Objects.equals(row.orderStatus,first.orderStatus) || row.receiptPresent!=first.receiptPresent)
                throw new IllegalArgumentException("INVALID_ORDER_SNAPSHOT");
        }
        return new OutboundDiagnosticDto(rows);
    }
}
