-- Existing V5 receipts remain queryable; their approval metadata is unknown.
-- New internal writes require a verified grant; unique issuer+grant binds one
-- committed cancellation across all warehouses (NULL legacy rows are retained).
ALTER TABLE wes_cancel_operation_receipt
    ADD COLUMN approval_issuer VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NULL,
    ADD COLUMN approval_grant_id VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NULL,
    ADD COLUMN approval_tenant VARCHAR(128) NULL,
    ADD COLUMN approval_requester VARCHAR(128) NULL,
    ADD COLUMN approval_reviewer VARCHAR(128) NULL,
    ADD COLUMN approval_valid_from_ms BIGINT NULL,
    ADD COLUMN approval_expires_at_ms BIGINT NULL,
    ADD UNIQUE KEY uk_cancel_approval_grant (approval_issuer, approval_grant_id);
