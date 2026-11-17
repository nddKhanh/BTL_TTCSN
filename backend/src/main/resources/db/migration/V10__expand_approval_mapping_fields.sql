ALTER TABLE order_approvals
    MODIFY approval_ref VARCHAR(100) NOT NULL;

ALTER TABLE approval_history
    MODIFY action VARCHAR(64) NOT NULL;
