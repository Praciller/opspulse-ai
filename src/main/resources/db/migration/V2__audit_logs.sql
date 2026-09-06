CREATE TABLE audit_logs (

    id UUID PRIMARY KEY,

    actor_user_id UUID,

    action VARCHAR(64) NOT NULL,

    entity_type VARCHAR(32) NOT NULL,

    entity_id UUID,

    before_snapshot JSONB,

    after_snapshot JSONB,

    request_id VARCHAR(64),

    ip_address VARCHAR(64),

    created_at TIMESTAMPTZ NOT NULL DEFAULT now()

);



CREATE INDEX idx_audit_logs_actor_created

    ON audit_logs (actor_user_id, created_at DESC);

CREATE INDEX idx_audit_logs_entity_created

    ON audit_logs (entity_type, entity_id, created_at DESC);

CREATE INDEX idx_audit_logs_request_id

    ON audit_logs (request_id);

