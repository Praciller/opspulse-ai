CREATE TABLE risk_events (
    id UUID PRIMARY KEY,
    risk_type VARCHAR(32) NOT NULL,
    severity VARCHAR(12) NOT NULL,
    entity_type VARCHAR(32) NOT NULL,
    entity_id UUID NOT NULL,
    source_metrics JSONB NOT NULL,
    explanation TEXT NOT NULL,
    recommended_action TEXT NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    resolved_at TIMESTAMPTZ,
    resolved_by UUID,
    organization_id UUID,
    dedup_key VARCHAR(128) GENERATED ALWAYS AS
        (risk_type || ':' || entity_type || ':' || entity_id::text) STORED,
    CONSTRAINT chk_risk_type CHECK (
        risk_type IN (
            'STOCKOUT_RISK', 'OVERSTOCK_RISK', 'SLOW_MOVING_INVENTORY',
            'ORDER_DELAY_RISK', 'SUPPLIER_DELAY_RISK', 'LOW_MARGIN_RISK'
        )
    ),
    CONSTRAINT chk_risk_severity CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT chk_risk_entity_type CHECK (entity_type IN ('PRODUCT', 'ORDER', 'SUPPLIER', 'PURCHASE_ORDER')),
    CONSTRAINT chk_risk_status CHECK (status IN ('OPEN', 'ACKNOWLEDGED', 'RESOLVED', 'DISMISSED')),
    CONSTRAINT chk_risk_source_metrics_object CHECK (jsonb_typeof(source_metrics) = 'object')
);

CREATE INDEX idx_risk_status_severity_created
    ON risk_events (status, severity, created_at DESC);
CREATE INDEX idx_risk_dedup_status
    ON risk_events (dedup_key, status);
CREATE INDEX idx_risk_entity
    ON risk_events (entity_type, entity_id);
CREATE UNIQUE INDEX uq_risk_active_dedup
    ON risk_events (dedup_key)
    WHERE status IN ('OPEN', 'ACKNOWLEDGED');

CREATE TABLE processed_events (
    event_id UUID PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
