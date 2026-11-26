package com.stone.wms.application.service;

import com.stone.wms.domain.repository.OutboundDiagnosticRepository;
import com.stone.wms.domain.repository.OutboundDiagnosticRepository.Row;
import com.stone.wms.interfaces.controller.OutboundDiagnosticController;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OutboundDiagnosticServiceTest {
    @Test void absentOrder() {
        OutboundDiagnosticRepository repo=mock(OutboundDiagnosticRepository.class);
        when(repo.snapshot(1L,9L)).thenReturn(Collections.emptyList());
        assertNull(new OutboundDiagnosticService(repo).query(1,9));
    }
    @Test void rejectsDuplicateLinesAndWrongWarehouse() {
        OutboundDiagnosticRepository repo=mock(OutboundDiagnosticRepository.class);
        Row r=new Row(); r.warehouseId=1; r.orderId=9; r.lineId=4L;
        when(repo.snapshot(1L,9L)).thenReturn(Arrays.asList(r,r));
        assertThrows(IllegalArgumentException.class,()->new OutboundDiagnosticService(repo).query(1,9));
        r.warehouseId=2;
        when(repo.snapshot(1L,9L)).thenReturn(Collections.singletonList(r));
        assertThrows(IllegalArgumentException.class,()->new OutboundDiagnosticService(repo).query(1,9));
    }
    @Test void rejectsUnauthorizedBeforeQuery() {
        OutboundDiagnosticService service=mock(OutboundDiagnosticService.class);
        String token="test-token-only-0123456789";
        OutboundDiagnosticController c=new OutboundDiagnosticController(service,token,"1");
        assertEquals(401,c.query(1,9,null).getStatusCodeValue());
        assertEquals(403,c.query(2,9,"Bearer "+token).getStatusCodeValue());
        verifyNoInteractions(service);
    }
    @Test void rejectsOversizedOrder() {
        OutboundDiagnosticRepository repo=mock(OutboundDiagnosticRepository.class);
        when(repo.snapshot(1L,9L)).thenReturn(Collections.nCopies(201,new Row()));
        assertThrows(IllegalArgumentException.class,()->new OutboundDiagnosticService(repo).query(1,9));
    }
}
