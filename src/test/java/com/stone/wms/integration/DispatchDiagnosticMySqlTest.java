package com.stone.wms.integration;

import com.stone.wms.application.dto.DispatchDiagnosticDto;
import com.stone.wms.application.service.DispatchDiagnosticService;
import com.stone.wms.startup.WmsApplication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="STONE_TEST_MYSQL_URL",matches=".+")
@SpringBootTest(classes=WmsApplication.class,properties={
    "spring.datasource.url=${STONE_TEST_MYSQL_URL}",
    "spring.datasource.username=${STONE_TEST_MYSQL_USER}",
    "spring.datasource.password=${STONE_TEST_MYSQL_PASSWORD}"
})
class DispatchDiagnosticMySqlTest {
    @Autowired JdbcTemplate sql;
    @Autowired DispatchDiagnosticService service;
    @Test void countsOnlyActiveTasksAndScopesDevicesWithoutWriting() {
        // Disposable V1-V4 database, distinct IDs. SQL fixture only; no physical dispatch.
        sql.update("INSERT INTO wes_dispatch_capacity(warehouse_id,max_ready,max_active,max_batch) VALUES(98001,2,4,8),(98002,10,16,8)");
        sql.update("INSERT INTO wes_dispatch_device(warehouse_id,device_code,max_active) VALUES(98001,'a',2),(98001,'b',4),(98001,'empty',2),(98002,'a',8)");
        sql.update("INSERT INTO wes_dispatch_task(warehouse_id,task_key,priority,state,device_code) VALUES"
            +"(98001,'r1',1,'READY',NULL),(98001,'r2',1,'READY',NULL),"
            +"(98001,'i1',1,'IN_FLIGHT','a'),(98001,'u1',1,'UNKNOWN','a'),"
            +"(98001,'u2',1,'UNKNOWN','b'),(98001,'s1',1,'SUCCEEDED','a'),"
            +"(98002,'other',1,'UNKNOWN','a')");
        DispatchDiagnosticDto d=service.query(98001,"a");
        assertNotNull(d); assertEquals(2,d.ready); assertEquals(1,d.inFlight);assertEquals(2,d.unknown);
        assertEquals(1,d.deviceInFlight);assertEquals(1,d.deviceUnknown);assertEquals(2,d.maxReady);
        DispatchDiagnosticDto empty=service.query(98001,"empty");assertEquals(0,empty.deviceInFlight+empty.deviceUnknown);
        assertNull(service.query(98001,"unregistered"));assertNull(service.query(98003,"a"));
        assertEquals(1,service.query(98002,"a").unknown);
        assertEquals(Integer.valueOf(0),sql.queryForObject("SELECT COUNT(*) FROM wes_claim WHERE warehouse_id=98001",Integer.class));
        assertEquals(Integer.valueOf(2),sql.queryForObject("SELECT COUNT(*) FROM wes_dispatch_task WHERE warehouse_id=98001 AND state='READY'",Integer.class));
    }
}
