CREATE EXTENSION IF NOT EXISTS citext;

CREATE TABLE users (
    id UUID PRIMARY KEY,
    email CITEXT NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(120) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    organization_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_users_email_lower ON users (lower(email));

CREATE TABLE roles (
    id UUID PRIMARY KEY,
    name VARCHAR(32) NOT NULL UNIQUE,
    CONSTRAINT chk_roles_name
        CHECK (name IN ('ADMIN', 'MANAGER', 'OPERATOR', 'VIEWER'))
);

INSERT INTO roles (id, name) VALUES
    ('00000000-0000-0000-0000-000000000001', 'ADMIN'),
    ('00000000-0000-0000-0000-000000000002', 'MANAGER'),
    ('00000000-0000-0000-0000-000000000003', 'OPERATOR'),
    ('00000000-0000-0000-0000-000000000004', 'VIEWER');

CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE RESTRICT,
    PRIMARY KEY (user_id, role_id)
);

CREATE INDEX idx_user_roles_role_id ON user_roles (role_id);

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    family_id UUID NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_refresh_tokens_token_hash
    ON refresh_tokens (token_hash);
CREATE INDEX idx_refresh_tokens_user_expires
    ON refresh_tokens (user_id, expires_at);
CREATE INDEX idx_refresh_tokens_family_id
    ON refresh_tokens (family_id);
