-- Apply only to reviewed V1-V4 schema. No HTTP cancellation submit endpoint in v0.9.1.
CREATE TABLE wes_cancel_operation_receipt (
    warehouse_id BIGINT NOT NULL,
    operation_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    task_id BIGINT NOT NULL,
    request_fingerprint CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    recorded_at DATETIME(6) NOT NULL,
    PRIMARY KEY (warehouse_id,operation_id),
    KEY ix_cancel_receipt_task (task_id),
    FOREIGN KEY (warehouse_id) REFERENCES wes_dispatch_capacity(warehouse_id),
    FOREIGN KEY (task_id) REFERENCES wes_dispatch_task(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
