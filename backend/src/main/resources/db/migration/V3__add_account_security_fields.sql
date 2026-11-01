ALTER TABLE users
    ADD COLUMN account_status VARCHAR(24) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN email_verified BIT(1) NOT NULL DEFAULT b'1',
    ADD COLUMN failed_login_count INT NOT NULL DEFAULT 0,
    ADD COLUMN locked_until DATETIME(6) NULL,
    ADD COLUMN password_changed_at DATETIME(6) NULL;
