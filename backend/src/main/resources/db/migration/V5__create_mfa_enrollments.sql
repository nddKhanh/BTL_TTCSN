CREATE TABLE mfa_enrollments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    encrypted_secret VARCHAR(512) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    recovery_code_hashes TEXT NULL,
    created_at DATETIME(6) NOT NULL,
    confirmed_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_mfa_enrollment_user UNIQUE (user_id),
    CONSTRAINT fk_mfa_enrollment_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
