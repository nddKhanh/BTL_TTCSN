CREATE TABLE security_audit_events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_type VARCHAR(50) NOT NULL,
    result VARCHAR(20) NOT NULL,
    actor_email VARCHAR(150) NULL,
    ip_address VARCHAR(64) NULL,
    user_agent VARCHAR(200) NULL,
    details VARCHAR(500) NULL,
    occurred_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_security_audit_filter (event_type, result, occurred_at)
);
