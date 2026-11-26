package com.stone.wms.application.service;

import com.stone.wms.application.dto.DispatchHistoryDto;
import com.stone.wms.domain.exception.DispatchException;
import com.stone.wms.domain.model.dispatch.DispatchTask;
import com.stone.wms.domain.model.inventory.StockWork;
import com.stone.wms.domain.repository.DispatchHistoryRepository;
import com.stone.wms.domain.repository.DispatchRepository;
import com.stone.wms.interfaces.controller.DispatchHistoryQueryController;
import org.junit.jupiter.api.Test;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DispatchHistoryQueryServiceTest {
    @Test void warehouseMissingDoesNotReadEvents() {
        DispatchRepository tasks=mock(DispatchRepository.class);
        DispatchHistoryRepository events=mock(DispatchHistoryRepository.class);
        assertThrows(DispatchException.class,()->new DispatchHistoryQueryService(tasks,events).query(2,103));
        verifyNoInteractions(events);
    }
    @Test void projectionAndBound() {
        DispatchRepository tasks=mock(DispatchRepository.class);
        DispatchHistoryRepository events=mock(DispatchHistoryRepository.class);
        StockWork work=new StockWork(StockWork.Type.TRANSFER,null,8,"L01",10L,20,12);
        when(tasks.task(1,103)).thenReturn(new DispatchTask(103,"demo",10,DispatchTask.State.UNKNOWN,"device","claim",work));
        DispatchHistoryRepository.Row row=new DispatchHistoryRepository.Row(); row.eventKey="e"; row.targetState="UNKNOWN";
        row.completedQty=4; row.reason="reported"; row.createdAt="2026-01-01T00:00:00.000000";
        when(events.events(1,103)).thenReturn(Collections.nCopies(101,row));
        DispatchHistoryDto out=new DispatchHistoryQueryService(tasks,events).query(1,103);
        assertEquals("UNKNOWN",out.taskState); assertEquals(100,out.events.size()); assertTrue(out.truncated); assertEquals("reported",out.events.get(0).reason);
    }
    @Test void controllerRejectsUnauthorizedBeforeQuery() {
        DispatchHistoryQueryService service=mock(DispatchHistoryQueryService.class);
        String token="development-token-1234567890";
        DispatchHistoryQueryController c=new DispatchHistoryQueryController(service,token,"1");
        assertEquals(401,c.query(1,103,null).getStatusCodeValue());
        assertEquals(403,c.query(2,103,"Bearer "+token).getStatusCodeValue());
        verifyNoInteractions(service);
    }
}
