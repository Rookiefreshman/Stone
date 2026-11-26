package com.stone.wms.integration;

import com.stone.wms.application.dto.OutboundDiagnosticDto;
import com.stone.wms.application.service.OutboundDiagnosticService;
import com.stone.wms.startup.WmsApplication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="STONE_TEST_MYSQL_URL", matches=".+")
@SpringBootTest(classes=WmsApplication.class,properties={
    "spring.datasource.url=${STONE_TEST_MYSQL_URL}",
    "spring.datasource.username=${STONE_TEST_MYSQL_USER}",
    "spring.datasource.password=${STONE_TEST_MYSQL_PASSWORD}"
})
class OutboundDiagnosticMySqlTest {
    @Autowired JdbcTemplate sql;
    @Autowired OutboundDiagnosticService service;
    @Test void scopedSnapshotMatchesAllocationDimensionsWithoutWriting() {
        // Disposable migrated test DB only. Separate ID range from existing dispatch tests.
        sql.update("INSERT INTO wms_outbound_order(id,order_no,warehouse_id,status) VALUES(97001,'TEST-OUT-97001',97001,'APPROVED')");
        sql.update("INSERT INTO wms_outbound_order_line(id,order_id,sku_id,location_id,lot_no,ordered_qty) VALUES(97001,97001,8,10,'L01',6),(97002,97001,8,10,'L01',5),(97003,97001,8,11,'L02',1)");
        sql.update("INSERT INTO wms_inventory(id,warehouse_id,location_id,sku_id,lot_no,inventory_status,on_hand_qty,available_qty) VALUES(97001,97001,10,8,'L01','AVAILABLE',10,10),(97002,97002,11,8,'L02','AVAILABLE',20,20)");
        sql.update("INSERT INTO wms_work_location(warehouse_id,location_id,max_units,mix_policy,freeze_key) VALUES(97001,10,100,'ANY','TEST-FREEZE')");
        OutboundDiagnosticDto d=service.query(97001,97001);
        assertNotNull(d); assertEquals(3,d.lines.size());
        assertEquals(d.lines.get(0).inventoryId,d.lines.get(1).inventoryId);
        assertEquals(Integer.valueOf(10),d.lines.get(0).availableQty);
        assertEquals("FROZEN",d.lines.get(0).locationState);
        assertNull(d.lines.get(2).inventoryId); assertEquals("UNCONFIGURED",d.lines.get(2).locationState);
        assertFalse(d.receiptPresent); assertNull(service.query(97002,97001));
        assertEquals(Integer.valueOf(10),sql.queryForObject("SELECT available_qty FROM wms_inventory WHERE id=97001",Integer.class));
        sql.update("INSERT INTO wms_allocation_receipt(order_id,idempotency_key) VALUES(97001,'test-only')");
        assertTrue(service.query(97001,97001).receiptPresent);
    }
}
