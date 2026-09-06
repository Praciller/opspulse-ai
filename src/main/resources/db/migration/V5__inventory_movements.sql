CREATE TABLE inventory_movements (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    movement_type VARCHAR(16) NOT NULL,
    quantity NUMERIC(18,3) NOT NULL,
    balance_before NUMERIC(18,3) NOT NULL,
    balance_after NUMERIC(18,3) NOT NULL,
    reason VARCHAR(200),
    reference_type VARCHAR(40),
    reference_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    request_id VARCHAR(64),
    CONSTRAINT chk_inventory_movement_type
        CHECK (movement_type IN ('INBOUND', 'OUTBOUND', 'ADJUSTMENT', 'RETURN')),
    CONSTRAINT chk_inventory_quantity_sign CHECK (
        (movement_type IN ('INBOUND', 'RETURN') AND quantity > 0)
        OR (movement_type = 'OUTBOUND' AND quantity < 0)
        OR (movement_type = 'ADJUSTMENT' AND quantity <> 0)
    ),
    CONSTRAINT chk_inventory_reference_pair CHECK (
        (reference_type IS NULL AND reference_id IS NULL)
        OR (reference_type IS NOT NULL AND reference_id IS NOT NULL)
    )
);

CREATE INDEX idx_movements_product_created
    ON inventory_movements (product_id, created_at DESC);
CREATE INDEX idx_movements_reference
    ON inventory_movements (reference_type, reference_id)
    WHERE reference_id IS NOT NULL;

CREATE FUNCTION reject_inventory_movement_mutation()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'inventory movements are immutable';
END;
$$;

CREATE TRIGGER inventory_movements_immutable
BEFORE UPDATE OR DELETE ON inventory_movements
FOR EACH ROW EXECUTE FUNCTION reject_inventory_movement_mutation();
