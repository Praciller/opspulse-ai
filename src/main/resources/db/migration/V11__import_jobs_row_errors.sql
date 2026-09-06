CREATE TABLE import_jobs (
    id UUID PRIMARY KEY,
    idempotency_key VARCHAR(128) NOT NULL UNIQUE,
    file_hash VARCHAR(64) NOT NULL,
    import_type VARCHAR(32) NOT NULL,
    organization_id UUID,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    total_rows INTEGER,
    success_rows INTEGER,
    error_rows INTEGER,
    started_at TIMESTAMPTZ,
    finished_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID NOT NULL,
    CONSTRAINT chk_import_type CHECK (import_type IN ('products', 'suppliers', 'orders', 'inventory', 'po')),
    CONSTRAINT chk_import_status CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')),
    CONSTRAINT chk_import_counts CHECK (
        (total_rows IS NULL OR total_rows >= 0)
        AND (success_rows IS NULL OR success_rows >= 0)
        AND (error_rows IS NULL OR error_rows >= 0)
    )
);

CREATE UNIQUE INDEX idx_import_jobs_content
    ON import_jobs (organization_id, import_type, file_hash) NULLS NOT DISTINCT;
CREATE INDEX idx_import_jobs_created
    ON import_jobs (created_at DESC);
CREATE INDEX idx_import_jobs_status_created
    ON import_jobs (status, created_at DESC);

CREATE TABLE import_row_errors (
    id UUID PRIMARY KEY,
    import_job_id UUID NOT NULL REFERENCES import_jobs(id) ON DELETE CASCADE,
    row_number INTEGER NOT NULL,
    raw_row TEXT NOT NULL,
    error_code VARCHAR(64) NOT NULL,
    error_message TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_import_row_number CHECK (row_number > 0),
    CONSTRAINT chk_import_error_message CHECK (length(error_message) <= 512)
);

CREATE INDEX idx_import_row_errors_job_row
    ON import_row_errors (import_job_id, row_number);
