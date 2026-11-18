CREATE TABLE wms_work_location (
    warehouse_id BIGINT NOT NULL,
    location_id BIGINT NOT NULL,
    max_units INT NOT NULL,
    mix_policy VARCHAR(16) NOT NULL,
    freeze_key VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin,
    PRIMARY KEY (warehouse_id, location_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE wms_location_freeze (
    warehouse_id BIGINT NOT NULL,
    location_id BIGINT NOT NULL,
    freeze_key VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    reason VARCHAR(240) NOT NULL,
    released_at DATETIME(6),
    PRIMARY KEY (warehouse_id, location_id, freeze_key),
    FOREIGN KEY (warehouse_id, location_id) REFERENCES wms_work_location(warehouse_id, location_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE wms_received_line (
    warehouse_id BIGINT NOT NULL,
    receipt_key VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    sku_id BIGINT NOT NULL,
    lot_no VARCHAR(64) NOT NULL,
    received_qty INT NOT NULL,
    PRIMARY KEY (warehouse_id, receipt_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE wms_stock_work (
    task_id BIGINT NOT NULL PRIMARY KEY,
    warehouse_id BIGINT NOT NULL,
    work_type VARCHAR(16) NOT NULL,
    receipt_key VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin,
    sku_id BIGINT NOT NULL,
    lot_no VARCHAR(64) NOT NULL,
    source_id BIGINT,
    destination_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    KEY ix_work_receipt (warehouse_id, receipt_key),
    KEY ix_work_source (warehouse_id, source_id),
    KEY ix_work_destination (warehouse_id, destination_id),
    FOREIGN KEY (task_id) REFERENCES wes_dispatch_task(id),
    FOREIGN KEY (warehouse_id, receipt_key) REFERENCES wms_received_line(warehouse_id, receipt_key),
    FOREIGN KEY (warehouse_id, source_id) REFERENCES wms_work_location(warehouse_id, location_id),
    FOREIGN KEY (warehouse_id, destination_id) REFERENCES wms_work_location(warehouse_id, location_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE wes_dispatch_event ADD COLUMN completed_qty INT NULL;

ALTER TABLE wes_dispatch_task
    ADD COLUMN priority_rank BIGINT GENERATED ALWAYS AS (-CAST(priority AS SIGNED)) STORED,
    ADD KEY ix_wes_queue_order (warehouse_id, state, priority_rank, id);

ALTER TABLE wms_inventory_transaction
    MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT,
    ADD COLUMN work_task_id BIGINT NULL,
    ADD COLUMN outbound_line_id BIGINT NULL,
    ADD COLUMN delta_available_qty INT NOT NULL DEFAULT 0,
    ADD COLUMN delta_reserved_qty INT NOT NULL DEFAULT 0,
    ADD COLUMN delta_frozen_qty INT NOT NULL DEFAULT 0,
    ADD UNIQUE KEY uk_txn_task_location_action (work_task_id, location_id, biz_type),
    ADD UNIQUE KEY uk_txn_outbound_line_action (outbound_line_id, biz_type);
