ALTER TABLE orders
    ADD COLUMN fulfillment_status VARCHAR(20) NULL AFTER status,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0 AFTER fulfillment_status;

UPDATE orders
SET fulfillment_status = CASE status
    WHEN 'PENDING' THEN 'NOT_STARTED'
    WHEN 'CONFIRMED' THEN 'PREPARING'
    WHEN 'DELIVERING' THEN 'DELIVERING'
    WHEN 'COMPLETED' THEN 'COMPLETED'
    WHEN 'CANCELLED' THEN 'CANCELLED'
END;

ALTER TABLE orders
    MODIFY fulfillment_status VARCHAR(20) NOT NULL;

CREATE TABLE order_approvals (
    id BIGINT NOT NULL AUTO_INCREMENT,
    approval_ref VARCHAR(36) NOT NULL,
    order_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    decided_by VARCHAR(100),
    decided_at DATETIME(6),
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_order_approvals_ref UNIQUE (approval_ref),
    CONSTRAINT uk_order_approvals_order UNIQUE (order_id),
    CONSTRAINT fk_order_approvals_order FOREIGN KEY (order_id) REFERENCES orders (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE approval_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    approval_id BIGINT NOT NULL,
    action VARCHAR(30) NOT NULL,
    actor_id VARCHAR(100) NOT NULL,
    from_status VARCHAR(20),
    to_status VARCHAR(20) NOT NULL,
    reason VARCHAR(500),
    created_at DATETIME(6) NOT NULL,
    request_id VARCHAR(100) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_approval_history_request UNIQUE (approval_id, request_id),
    CONSTRAINT fk_approval_history_approval FOREIGN KEY (approval_id) REFERENCES order_approvals (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE fulfillment_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    action VARCHAR(30) NOT NULL,
    actor_id VARCHAR(100) NOT NULL,
    from_status VARCHAR(20) NOT NULL,
    to_status VARCHAR(20) NOT NULL,
    reason VARCHAR(500),
    created_at DATETIME(6) NOT NULL,
    request_id VARCHAR(100) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_fulfillment_history_request UNIQUE (order_id, request_id),
    CONSTRAINT fk_fulfillment_history_order FOREIGN KEY (order_id) REFERENCES orders (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO order_approvals (
    approval_ref,
    order_id,
    status,
    created_by,
    created_at,
    decided_by,
    decided_at,
    version
)
SELECT
    UUID(),
    id,
    CASE status
        WHEN 'PENDING' THEN 'PENDING'
        WHEN 'CANCELLED' THEN 'WITHDRAWN'
        ELSE 'ACCEPTED'
    END,
    CASE
        WHEN user_id IS NULL THEN CONCAT('guest-order:', order_code)
        ELSE CONCAT('user:', user_id)
    END,
    COALESCE(created_at, CURRENT_TIMESTAMP(6)),
    CASE WHEN status = 'PENDING' THEN NULL ELSE 'migration' END,
    CASE WHEN status = 'PENDING' THEN NULL ELSE COALESCE(created_at, CURRENT_TIMESTAMP(6)) END,
    0
FROM orders;

INSERT INTO approval_history (
    approval_id,
    action,
    actor_id,
    from_status,
    to_status,
    reason,
    created_at,
    request_id
)
SELECT
    approval.id,
    'MIGRATE',
    'migration',
    NULL,
    approval.status,
    'Backfill from legacy OrderStatus',
    approval.created_at,
    CONCAT('migration:', orders.order_code)
FROM order_approvals approval
JOIN orders ON orders.id = approval.order_id;

INSERT INTO fulfillment_history (
    order_id,
    action,
    actor_id,
    from_status,
    to_status,
    reason,
    created_at,
    request_id
)
SELECT
    id,
    'MIGRATE',
    'migration',
    fulfillment_status,
    fulfillment_status,
    'Backfill from legacy OrderStatus',
    COALESCE(created_at, CURRENT_TIMESTAMP(6)),
    CONCAT('migration:', order_code)
FROM orders;

INSERT INTO role_permissions (role_name, permission_name) VALUES
    ('ADMIN', 'ORDER_APPROVE'),
    ('ADMIN', 'ORDER_FULFILL');

CREATE INDEX idx_order_approvals_status ON order_approvals (status);
CREATE INDEX idx_approval_history_created_at ON approval_history (created_at);
CREATE INDEX idx_fulfillment_history_created_at ON fulfillment_history (created_at);
