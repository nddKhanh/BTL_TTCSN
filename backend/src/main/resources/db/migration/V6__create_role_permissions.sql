CREATE TABLE role_permissions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    role_name VARCHAR(20) NOT NULL,
    permission_name VARCHAR(40) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_role_permission UNIQUE (role_name, permission_name)
);

INSERT INTO role_permissions (role_name, permission_name) VALUES
('CUSTOMER', 'PROFILE_READ'),
('CUSTOMER', 'PROFILE_WRITE'),
('CUSTOMER', 'ORDER_CREATE'),
('CUSTOMER', 'ORDER_READ_OWN'),
('ADMIN', 'PROFILE_READ'),
('ADMIN', 'PROFILE_WRITE'),
('ADMIN', 'ORDER_CREATE'),
('ADMIN', 'ORDER_READ_OWN'),
('ADMIN', 'ORDER_READ'),
('ADMIN', 'CATALOG_WRITE'),
('ADMIN', 'COUPON_WRITE'),
('ADMIN', 'DASHBOARD_READ'),
('ADMIN', 'REPORT_READ');
