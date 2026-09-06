CREATE TABLE orders (
    id UUID PRIMARY KEY,
    order_number VARCHAR(64) NOT NULL UNIQUE,
    customer_name VARCHAR(200) NOT NULL,
    status VARCHAR(16) NOT NULL,
    expected_ship_date DATE NOT NULL,
    actual_ship_date DATE,
    total_amount NUMERIC(18,4) NOT NULL DEFAULT 0,
    organization_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_orders_number_not_blank CHECK (length(trim(order_number)) > 0),
    CONSTRAINT chk_orders_customer_not_blank CHECK (length(trim(customer_name)) > 0),
    CONSTRAINT chk_orders_status CHECK (
        status IN ('NEW', 'CONFIRMED', 'PICKING', 'SHIPPED', 'DELAYED', 'CANCELLED')
    ),
    CONSTRAINT chk_orders_total_nonnegative CHECK (total_amount >= 0),
    CONSTRAINT chk_orders_actual_ship_status CHECK (
        (status = 'SHIPPED' AND actual_ship_date IS NOT NULL)
        OR (status <> 'SHIPPED' AND actual_ship_date IS NULL)
    )
);

CREATE INDEX idx_orders_status_shipdate ON orders (status, expected_ship_date);
CREATE INDEX idx_orders_customer_lower ON orders (lower(customer_name));

CREATE TABLE order_items (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    quantity NUMERIC(18,3) NOT NULL,
    unit_price NUMERIC(18,4) NOT NULL,
    line_total NUMERIC(18,4)
        GENERATED ALWAYS AS (round(quantity * unit_price, 4)) STORED,
    CONSTRAINT chk_order_items_quantity_positive CHECK (quantity > 0),
    CONSTRAINT chk_order_items_price_nonnegative CHECK (unit_price >= 0),
    CONSTRAINT uq_order_items_product UNIQUE (order_id, product_id)
);

CREATE INDEX idx_order_items_order ON order_items (order_id);
CREATE INDEX idx_order_items_product ON order_items (product_id);
