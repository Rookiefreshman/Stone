CREATE TABLE wms_allocation_receipt (
    order_id BIGINT NOT NULL PRIMARY KEY,
    idempotency_key VARCHAR(128) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    FOREIGN KEY (order_id) REFERENCES wms_outbound_order (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
