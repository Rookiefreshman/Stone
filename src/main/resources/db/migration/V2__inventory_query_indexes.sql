-- Query and audit indexes for inventory read paths and stock movement tracing.
CREATE INDEX idx_inventory_balance_sku_location
    ON inventory_balance (sku_id, warehouse_id, location_id);

CREATE INDEX idx_inventory_balance_status_quantity
    ON inventory_balance (warehouse_id, inventory_status, quantity);

CREATE INDEX idx_stock_movement_reference
    ON stock_movement (reference_no, movement_type);

CREATE INDEX idx_stock_movement_sku_time
    ON stock_movement (sku_id, occurred_at);

CREATE INDEX idx_outbox_event_status_created
    ON outbox_event (status, created_at);
