CREATE TABLE prompt_versions (
    version VARCHAR(32) PRIMARY KEY,
    template TEXT NOT NULL,
    description TEXT,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO prompt_versions (version, template, description, active)
VALUES (
    '1.0.0',
    'You are an operations analyst. Use only the supplied risk events and source metrics. Return JSON with summary, actions, messageDrafts, confidence, and riskEventIds. Never invent metrics, entities, or risk IDs. Prioritize the highest severity findings and explain each action using the supplied evidence.',
    'Deterministic-first operations brief prompt',
    true
)
ON CONFLICT (version) DO NOTHING;

CREATE UNIQUE INDEX uq_prompt_versions_active
    ON prompt_versions (active)
    WHERE active;

CREATE TABLE ai_recommendations (
    id UUID PRIMARY KEY,
    prompt_version VARCHAR(32) NOT NULL REFERENCES prompt_versions(version),
    model_provider_name VARCHAR(64) NOT NULL,
    generated_by VARCHAR(16) NOT NULL,
    generated_summary TEXT NOT NULL,
    generated_actions JSONB NOT NULL,
    generated_message_drafts JSONB NOT NULL,
    confidence NUMERIC(4,3),
    status VARCHAR(16) NOT NULL DEFAULT 'GENERATED',
    user_feedback TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    created_by_type VARCHAR(16) NOT NULL DEFAULT 'USER',
    organization_id UUID,
    CONSTRAINT chk_ai_generated_by CHECK (generated_by IN ('AI', 'RULE_BASED')),
    CONSTRAINT chk_ai_recommendation_status CHECK (status IN ('GENERATED', 'APPROVED', 'REJECTED', 'ARCHIVED')),
    CONSTRAINT chk_ai_created_by_type CHECK (created_by_type IN ('USER', 'SYSTEM')),
    CONSTRAINT chk_ai_confidence CHECK (confidence IS NULL OR (confidence >= 0 AND confidence <= 1)),
    CONSTRAINT chk_ai_actions_array CHECK (jsonb_typeof(generated_actions) = 'array'),
    CONSTRAINT chk_ai_message_drafts_array CHECK (jsonb_typeof(generated_message_drafts) = 'array')
);

CREATE INDEX idx_ai_recommendations_created
    ON ai_recommendations (created_at DESC);
CREATE INDEX idx_ai_recommendations_status_created
    ON ai_recommendations (status, created_at DESC);

CREATE TABLE ai_recommendation_items (
    id UUID PRIMARY KEY,
    recommendation_id UUID NOT NULL REFERENCES ai_recommendations(id) ON DELETE CASCADE,
    risk_event_id UUID REFERENCES risk_events(id) ON DELETE SET NULL,
    role VARCHAR(32) NOT NULL
);

CREATE UNIQUE INDEX uq_ai_recommendation_risk
    ON ai_recommendation_items (recommendation_id, risk_event_id);
CREATE INDEX idx_ai_items_risk_event
    ON ai_recommendation_items (risk_event_id);

CREATE TABLE ai_usage_audit (
    id UUID PRIMARY KEY,
    recommendation_id UUID REFERENCES ai_recommendations(id) ON DELETE SET NULL,
    provider VARCHAR(64) NOT NULL,
    model VARCHAR(64) NOT NULL,
    prompt_version VARCHAR(32) NOT NULL REFERENCES prompt_versions(version),
    input_token_count INTEGER,
    output_token_count INTEGER,
    latency_ms INTEGER,
    success BOOLEAN NOT NULL,
    error_class VARCHAR(120),
    request_id VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_ai_usage_token_counts CHECK (
        (input_token_count IS NULL OR input_token_count >= 0)
        AND (output_token_count IS NULL OR output_token_count >= 0)
    ),
    CONSTRAINT chk_ai_usage_latency CHECK (latency_ms IS NULL OR latency_ms >= 0)
);

CREATE INDEX idx_ai_usage_recommendation
    ON ai_usage_audit (recommendation_id, created_at DESC);
