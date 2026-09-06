CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(32) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(12) NOT NULL DEFAULT 'NEW',
    retry_count INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    processed_at TIMESTAMPTZ,
    CONSTRAINT chk_outbox_status CHECK (status IN ('NEW', 'RETRY', 'PROCESSED', 'DEAD')),
    CONSTRAINT chk_outbox_retry_nonnegative CHECK (retry_count >= 0),
    CONSTRAINT chk_outbox_payload_object CHECK (jsonb_typeof(payload) = 'object')
);

CREATE INDEX idx_outbox_status_next
    ON outbox_events (status, next_attempt_at)
    WHERE status IN ('NEW', 'RETRY');
CREATE INDEX idx_outbox_aggregate
    ON outbox_events (aggregate_type, aggregate_id, created_at DESC);
