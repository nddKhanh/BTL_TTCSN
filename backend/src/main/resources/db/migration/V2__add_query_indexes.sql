-- UNIQUE constraints in V1 already provide indexes for orders.order_code,
-- users.email and coupons.code. Add the remaining query indexes here.
CREATE INDEX idx_orders_created_at ON orders (created_at);
CREATE INDEX idx_orders_status ON orders (status);
CREATE INDEX idx_products_name ON products (name);
