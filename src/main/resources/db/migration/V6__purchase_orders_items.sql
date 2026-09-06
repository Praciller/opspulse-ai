CREATE TABLE purchase_orders (
    id UUID PRIMARY KEY,
    po_number VARCHAR(64) NOT NULL UNIQUE,
    supplier_id UUID NOT NULL REFERENCES suppliers(id) ON DELETE RESTRICT,
    status VARCHAR(20) NOT NULL,
    expected_delivery_date DATE NOT NULL,
    actual_delivery_date DATE,
    organization_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_purchase_orders_number_not_blank CHECK (length(trim(po_number)) > 0),
    CONSTRAINT chk_purchase_orders_status CHECK (
        status IN ('DRAFT', 'SENT', 'PARTIALLY_RECEIVED', 'RECEIVED', 'DELAYED', 'CANCELLED')
    ),
    CONSTRAINT chk_purchase_orders_actual_date CHECK (
        (status = 'RECEIVED' AND actual_delivery_date IS NOT NULL)
        OR (status <> 'RECEIVED' AND actual_delivery_date IS NULL)
    )
);

CREATE INDEX idx_po_supplier_status ON purchase_orders (supplier_id, status);
CREATE INDEX idx_po_status_expected ON purchase_orders (status, expected_delivery_date);

CREATE TABLE purchase_order_items (
    id UUID PRIMARY KEY,
    purchase_order_id UUID NOT NULL REFERENCES purchase_orders(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    quantity NUMERIC(18,3) NOT NULL,
    unit_cost NUMERIC(18,4) NOT NULL,
    received_quantity NUMERIC(18,3) NOT NULL DEFAULT 0,
    CONSTRAINT chk_po_items_quantity_positive CHECK (quantity > 0),
    CONSTRAINT chk_po_items_cost_nonnegative CHECK (unit_cost >= 0),
    CONSTRAINT chk_po_items_received_range
        CHECK (received_quantity >= 0 AND received_quantity <= quantity),
    CONSTRAINT uq_po_items_product UNIQUE (purchase_order_id, product_id)
);

CREATE INDEX idx_po_items_po ON purchase_order_items (purchase_order_id);
CREATE INDEX idx_po_items_product ON purchase_order_items (product_id);
