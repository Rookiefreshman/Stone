CREATE TABLE IF NOT EXISTS wms_inventory (
    id BIGINT NOT NULL PRIMARY KEY,
    warehouse_id BIGINT NOT NULL,
    location_id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    lot_no VARCHAR(64) NOT NULL DEFAULT '',
    inventory_status VARCHAR(32) NOT NULL,
    on_hand_qty INT NOT NULL DEFAULT 0,
    available_qty INT NOT NULL DEFAULT 0,
    reserved_qty INT NOT NULL DEFAULT 0,
    frozen_qty INT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_inventory_dimension (warehouse_id, location_id, sku_id, lot_no, inventory_status, deleted),
    KEY idx_inventory_sku (warehouse_id, sku_id, deleted),
    KEY idx_inventory_location (warehouse_id, location_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS wms_outbound_order (
    id BIGINT NOT NULL PRIMARY KEY,
    order_no VARCHAR(64) NOT NULL,
    warehouse_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_outbound_order_no (order_no),
    KEY idx_outbound_status (warehouse_id, status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS wms_outbound_order_line (
    id BIGINT NOT NULL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    location_id BIGINT NOT NULL,
    lot_no VARCHAR(64) NOT NULL DEFAULT '',
    ordered_qty INT NOT NULL,
    allocated_qty INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    KEY idx_outbound_line_order (order_id, deleted),
    KEY idx_outbound_line_sku (sku_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS wms_inventory_transaction (
    id BIGINT NOT NULL PRIMARY KEY,
    warehouse_id BIGINT NOT NULL,
    location_id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    lot_no VARCHAR(64) NOT NULL DEFAULT '',
    biz_type VARCHAR(32) NOT NULL,
    biz_no VARCHAR(64) NOT NULL,
    quantity INT NOT NULL,
    before_on_hand_qty INT NOT NULL,
    after_on_hand_qty INT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    KEY idx_txn_biz (biz_type, biz_no),
    KEY idx_txn_sku (warehouse_id, sku_id, created_at),
    KEY idx_txn_location (warehouse_id, location_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
