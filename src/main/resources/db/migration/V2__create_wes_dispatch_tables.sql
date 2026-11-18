CREATE TABLE wes_dispatch_capacity (
    warehouse_id BIGINT NOT NULL PRIMARY KEY,
    max_ready INT UNSIGNED NOT NULL,
    max_active INT UNSIGNED NOT NULL,
    max_batch INT UNSIGNED NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE wes_dispatch_device (
    warehouse_id BIGINT NOT NULL,
    device_code VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    max_active INT UNSIGNED NOT NULL,
    PRIMARY KEY (warehouse_id, device_code),
    FOREIGN KEY (warehouse_id) REFERENCES wes_dispatch_capacity (warehouse_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE wes_claim (
    warehouse_id BIGINT NOT NULL,
    claim_key VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    device_code VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    requested_limit INT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (warehouse_id, claim_key),
    FOREIGN KEY (warehouse_id, device_code) REFERENCES wes_dispatch_device (warehouse_id, device_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE wes_dispatch_task (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    warehouse_id BIGINT NOT NULL,
    task_key VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    priority INT NOT NULL,
    state VARCHAR(16) NOT NULL,
    device_code VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin,
    claim_key VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_wes_task (warehouse_id, task_key),
    KEY ix_wes_queue (warehouse_id, state, priority, id),
    KEY ix_wes_active (warehouse_id, state, device_code),
    KEY ix_wes_task_claim (warehouse_id, claim_key, id),
    FOREIGN KEY (warehouse_id) REFERENCES wes_dispatch_capacity (warehouse_id),
    FOREIGN KEY (warehouse_id, claim_key) REFERENCES wes_claim (warehouse_id, claim_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE wes_dispatch_event (
    task_id BIGINT NOT NULL,
    event_key VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    target_state VARCHAR(16) NOT NULL,
    reconciled TINYINT NOT NULL,
    reason VARCHAR(240),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (task_id, event_key),
    FOREIGN KEY (task_id) REFERENCES wes_dispatch_task (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
