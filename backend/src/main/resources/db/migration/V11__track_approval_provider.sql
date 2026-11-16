ALTER TABLE order_approvals
    ADD COLUMN provider VARCHAR(16) NOT NULL DEFAULT 'LOCAL' AFTER approval_ref,
    ADD COLUMN local_approval_ref VARCHAR(100) NULL AFTER provider,
    ADD COLUMN migration_completed_at DATETIME(6) NULL AFTER local_approval_ref;

UPDATE order_approvals
SET provider = CASE
    WHEN approval_ref REGEXP '^[0-9]+$' THEN 'DXOS'
    ELSE 'LOCAL'
END;

CREATE INDEX idx_order_approvals_provider ON order_approvals (provider);
