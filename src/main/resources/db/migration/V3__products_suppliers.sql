CREATE TABLE products (
    id UUID PRIMARY KEY,
    sku VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    category VARCHAR(80),
    unit VARCHAR(20) NOT NULL DEFAULT 'PCS',
    current_stock NUMERIC(18,3) NOT NULL DEFAULT 0,
    safety_stock NUMERIC(18,3) NOT NULL DEFAULT 0,
    reorder_point NUMERIC(18,3) NOT NULL DEFAULT 0,
    cost NUMERIC(18,4) NOT NULL DEFAULT 0,
    selling_price NUMERIC(18,4) NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    organization_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_products_sku_normalized CHECK (sku = upper(trim(sku))),
    CONSTRAINT chk_products_name_not_blank CHECK (length(trim(name)) > 0),
    CONSTRAINT chk_products_unit_not_blank CHECK (length(trim(unit)) > 0),
    CONSTRAINT chk_products_safety_stock_nonnegative CHECK (safety_stock >= 0),
    CONSTRAINT chk_products_reorder_point_nonnegative CHECK (reorder_point >= 0),
    CONSTRAINT chk_products_cost_nonnegative CHECK (cost >= 0),
    CONSTRAINT chk_products_selling_price_nonnegative CHECK (selling_price >= 0)
);

CREATE INDEX idx_products_active_category ON products (active, category);
CREATE INDEX idx_products_name_lower ON products (lower(name));

CREATE TABLE suppliers (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    contact_info JSONB,
    average_lead_time_days NUMERIC(6,2),
    expected_sla_days INTEGER NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    organization_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_suppliers_name_not_blank CHECK (length(trim(name)) > 0),
    CONSTRAINT chk_suppliers_contact_object
        CHECK (contact_info IS NULL OR jsonb_typeof(contact_info) = 'object'),
    CONSTRAINT chk_suppliers_average_lead_nonnegative
        CHECK (average_lead_time_days IS NULL OR average_lead_time_days >= 0),
    CONSTRAINT chk_suppliers_sla_nonnegative CHECK (expected_sla_days >= 0)
);

CREATE INDEX idx_suppliers_active_name ON suppliers (active, lower(name));
