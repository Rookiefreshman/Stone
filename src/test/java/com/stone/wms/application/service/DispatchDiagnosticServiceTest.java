package com.stone.wms.application.service;

import com.stone.wms.application.dto.DispatchDiagnosticDto;
import com.stone.wms.domain.repository.DispatchDiagnosticRepository;
import com.stone.wms.domain.repository.DispatchDiagnosticRepository.Row;
import com.stone.wms.interfaces.controller.DispatchDiagnosticController;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DispatchDiagnosticServiceTest {
    private Row row() {
        Row r=new Row(); r.warehouseId=1; r.deviceCode="station-01"; r.maxReady=200; r.maxActive=16; r.maxBatch=8;
        r.deviceMaxActive=4; r.ready=25; r.inFlight=9; r.unknown=4; r.deviceInFlight=2; r.deviceUnknown=2;
        return r;
    }
    @Test void absentScope() {
        DispatchDiagnosticRepository repo=mock(DispatchDiagnosticRepository.class);
        assertNull(new DispatchDiagnosticService(repo).query(1,"station-01"));
    }
    @Test void projectionAllowsOverCapacityAfterDownsize() {
        DispatchDiagnosticRepository repo=mock(DispatchDiagnosticRepository.class);
        Row r=row();r.maxActive=10;r.deviceMaxActive=2;
        when(repo.snapshot(1,"station-01")).thenReturn(r);
        DispatchDiagnosticDto d=new DispatchDiagnosticService(repo).query(1,"station-01");
        assertEquals(13,d.inFlight+d.unknown); assertEquals(4,d.deviceInFlight+d.deviceUnknown);
    }
    @Test void rejectsInvalidAndWrongScopeCounts() {
        DispatchDiagnosticRepository repo=mock(DispatchDiagnosticRepository.class);
        Row r=row();r.deviceUnknown=5;
        when(repo.snapshot(1,"station-01")).thenReturn(r);
        DispatchDiagnosticService service=new DispatchDiagnosticService(repo);
        assertThrows(IllegalArgumentException.class,()->service.query(1,"station-01"));
        r.deviceUnknown=2;r.warehouseId=2;
        assertThrows(IllegalArgumentException.class,()->service.query(1,"station-01"));
        r.warehouseId=1;r.maxActive=2147483648L;
        assertThrows(IllegalArgumentException.class,()->service.query(1,"station-01"));
    }
    @Test void unauthorizedNeverReadsRepository() {
        DispatchDiagnosticService service=mock(DispatchDiagnosticService.class);
        String token="unit-test-token-only-0123456789";
        DispatchDiagnosticController c=new DispatchDiagnosticController(service,token,"1");
        assertEquals(401,c.query(1,"station-01",null).getStatusCodeValue());
        assertEquals(403,c.query(2,"station-01","Bearer "+token).getStatusCodeValue());
        assertEquals(400,c.query(1,"station?x=1","Bearer "+token).getStatusCodeValue());
        verifyNoInteractions(service);
    }
    @Test void distinguishesMissingScopeFromEmptyQueue() {
        DispatchDiagnosticService service=mock(DispatchDiagnosticService.class);
        String token="unit-test-token-only-0123456789";
        DispatchDiagnosticController c=new DispatchDiagnosticController(service,token,"1");
        assertEquals(404,c.query(1,"station-01","Bearer "+token).getStatusCodeValue());
        Row r=row();r.ready=r.inFlight=r.unknown=r.deviceInFlight=r.deviceUnknown=0;
        when(service.query(1,"station-01")).thenReturn(new DispatchDiagnosticDto(r));
        assertEquals(200,c.query(1,"station-01","Bearer "+token).getStatusCodeValue());
    }
}
