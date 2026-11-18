CREATE TABLE IF NOT EXISTS warehouse (
    id VARCHAR(36) PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS sku (
    id VARCHAR(36) PRIMARY KEY,
    warehouse_id VARCHAR(36) NOT NULL,
    code VARCHAR(40) NOT NULL,
    name VARCHAR(100) NOT NULL,
    UNIQUE (warehouse_id, code),
    FOREIGN KEY (warehouse_id) REFERENCES warehouse(id)
);

CREATE TABLE IF NOT EXISTS location (
    id VARCHAR(36) PRIMARY KEY,
    warehouse_id VARCHAR(36) NOT NULL,
    code VARCHAR(40) NOT NULL,
    name VARCHAR(100) NOT NULL,
    location_type VARCHAR(16) NOT NULL,
    UNIQUE (warehouse_id, code),
    FOREIGN KEY (warehouse_id) REFERENCES warehouse(id)
);

CREATE TABLE IF NOT EXISTS lot (
    id VARCHAR(36) PRIMARY KEY,
    sku_id VARCHAR(36) NOT NULL,
    batch_code VARCHAR(40) NOT NULL,
    expires_on DATE,
    UNIQUE (sku_id, batch_code),
    FOREIGN KEY (sku_id) REFERENCES sku(id)
);

CREATE TABLE IF NOT EXISTS stock (
    location_id VARCHAR(36) NOT NULL,
    lot_id VARCHAR(36) NOT NULL,
    on_hand BIGINT NOT NULL,
    reserved BIGINT NOT NULL,
    PRIMARY KEY (location_id, lot_id),
    FOREIGN KEY (location_id) REFERENCES location(id),
    FOREIGN KEY (lot_id) REFERENCES lot(id)
);

CREATE TABLE IF NOT EXISTS document (
    id VARCHAR(36) PRIMARY KEY,
    warehouse_id VARCHAR(36) NOT NULL,
    kind VARCHAR(16) NOT NULL,
    external_ref VARCHAR(64) NOT NULL,
    source_document_id VARCHAR(36),
    state VARCHAR(24) NOT NULL,
    request_hash CHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (warehouse_id, kind, external_ref),
    INDEX ix_document_board (warehouse_id, created_at, id),
    INDEX ix_document_source (source_document_id, state),
    FOREIGN KEY (source_document_id) REFERENCES document(id),
    FOREIGN KEY (warehouse_id) REFERENCES warehouse(id)
);

CREATE TABLE IF NOT EXISTS document_line (
    id VARCHAR(36) PRIMARY KEY,
    document_id VARCHAR(36) NOT NULL,
    line_no INT NOT NULL,
    sku_id VARCHAR(36) NOT NULL,
    lot_id VARCHAR(36),
    source_id VARCHAR(36),
    destination_id VARCHAR(36),
    quantity BIGINT NOT NULL,
    UNIQUE (document_id, line_no),
    FOREIGN KEY (document_id) REFERENCES document(id),
    FOREIGN KEY (sku_id) REFERENCES sku(id),
    FOREIGN KEY (lot_id) REFERENCES lot(id),
    FOREIGN KEY (source_id) REFERENCES location(id),
    FOREIGN KEY (destination_id) REFERENCES location(id)
);

CREATE TABLE IF NOT EXISTS task (
    id VARCHAR(36) PRIMARY KEY,
    warehouse_id VARCHAR(36) NOT NULL,
    document_id VARCHAR(36) NOT NULL,
    line_id VARCHAR(36) NOT NULL,
    lot_id VARCHAR(36) NOT NULL,
    source_id VARCHAR(36),
    destination_id VARCHAR(36),
    quantity BIGINT NOT NULL,
    observed_quantity BIGINT,
    state VARCHAR(24) NOT NULL,
    attempt INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX ix_task_queue (warehouse_id, state, id),
    INDEX ix_task_document (document_id, id),
    INDEX ix_task_count_lock (warehouse_id, lot_id, source_id, state),
    FOREIGN KEY (warehouse_id) REFERENCES warehouse(id),
    FOREIGN KEY (document_id) REFERENCES document(id),
    FOREIGN KEY (line_id) REFERENCES document_line(id),
    FOREIGN KEY (lot_id) REFERENCES lot(id),
    FOREIGN KEY (source_id) REFERENCES location(id),
    FOREIGN KEY (destination_id) REFERENCES location(id)
);

CREATE TABLE IF NOT EXISTS device (
    id VARCHAR(36) PRIMARY KEY,
    warehouse_id VARCHAR(36) NOT NULL,
    code VARCHAR(40) NOT NULL,
    name VARCHAR(100) NOT NULL,
    device_mode VARCHAR(16) NOT NULL,
    state VARCHAR(16) NOT NULL,
    UNIQUE (warehouse_id, code),
    FOREIGN KEY (warehouse_id) REFERENCES warehouse(id)
);

CREATE TABLE IF NOT EXISTS device_command (
    id VARCHAR(36) PRIMARY KEY,
    task_id VARCHAR(36) NOT NULL,
    device_id VARCHAR(36) NOT NULL,
    attempt INT NOT NULL,
    state VARCHAR(16) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (task_id, attempt),
    INDEX ix_command_device (device_id, state),
    FOREIGN KEY (task_id) REFERENCES task(id),
    FOREIGN KEY (device_id) REFERENCES device(id)
);

CREATE TABLE IF NOT EXISTS command_event (
    command_id VARCHAR(36) NOT NULL,
    event_key VARCHAR(64) NOT NULL,
    event_type VARCHAR(16) NOT NULL,
    observed_quantity BIGINT,
    detail VARCHAR(240),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (command_id, event_key),
    FOREIGN KEY (command_id) REFERENCES device_command(id)
);

CREATE TABLE IF NOT EXISTS inventory_ledger (
    id VARCHAR(36) PRIMARY KEY,
    warehouse_id VARCHAR(36) NOT NULL,
    task_id VARCHAR(36) NOT NULL,
    location_id VARCHAR(36) NOT NULL,
    lot_id VARCHAR(36) NOT NULL,
    reason VARCHAR(24) NOT NULL,
    delta_on_hand BIGINT NOT NULL,
    delta_reserved BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (task_id, location_id, reason),
    INDEX ix_ledger_board (warehouse_id, created_at, id),
    FOREIGN KEY (warehouse_id) REFERENCES warehouse(id),
    FOREIGN KEY (task_id) REFERENCES task(id),
    FOREIGN KEY (location_id) REFERENCES location(id),
    FOREIGN KEY (lot_id) REFERENCES lot(id)
);

CREATE TABLE IF NOT EXISTS operation_audit (
    id VARCHAR(36) PRIMARY KEY,
    warehouse_id VARCHAR(36) NOT NULL,
    document_id VARCHAR(36) NOT NULL,
    task_id VARCHAR(36),
    action VARCHAR(24) NOT NULL,
    detail VARCHAR(240) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX ix_audit_document (document_id, created_at, id),
    FOREIGN KEY (warehouse_id) REFERENCES warehouse(id),
    FOREIGN KEY (document_id) REFERENCES document(id),
    FOREIGN KEY (task_id) REFERENCES task(id)
);

CREATE TABLE IF NOT EXISTS outbound_wave (
    id VARCHAR(36) PRIMARY KEY,
    warehouse_id VARCHAR(36) NOT NULL,
    code VARCHAR(40) NOT NULL,
    priority INT NOT NULL,
    state VARCHAR(16) NOT NULL,
    reason VARCHAR(240),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (warehouse_id, code),
    INDEX ix_wave_board (warehouse_id, created_at, id),
    FOREIGN KEY (warehouse_id) REFERENCES warehouse(id)
);

CREATE TABLE IF NOT EXISTS wave_document (
    wave_id VARCHAR(36) NOT NULL,
    document_id VARCHAR(36) NOT NULL,
    sequence_no INT NOT NULL,
    PRIMARY KEY (wave_id, document_id),
    INDEX ix_wave_document_reverse (document_id),
    FOREIGN KEY (wave_id) REFERENCES outbound_wave(id),
    FOREIGN KEY (document_id) REFERENCES document(id)
);
