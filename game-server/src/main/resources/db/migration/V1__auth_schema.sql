-- Auth schema: persistent user accounts keyed by (provider, subject).
-- Each OAuth provider gets a separate account row per human; display names are not unique,
-- but the (lower(display_name), discriminator) pair is — Discord-style handles like "Alice#0042".
-- See backlog/oauth2-accounts.md for the full design.

CREATE SCHEMA IF NOT EXISTS auth;

CREATE TABLE auth.user_accounts (
    id              UUID PRIMARY KEY,
    provider        VARCHAR(32)  NOT NULL,
    subject         VARCHAR(128) NOT NULL,
    email           VARCHAR(320) NOT NULL,
    display_name    VARCHAR(80)  NOT NULL,
    discriminator   SMALLINT     NOT NULL CHECK (discriminator BETWEEN 1 AND 9999),
    avatar_url      TEXT,
    created_at      TIMESTAMPTZ  NOT NULL,
    last_login_at   TIMESTAMPTZ  NOT NULL,
    status          VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    UNIQUE (provider, subject)
);

-- Case-insensitive email lookup (provisional admin allowlist + future tooling).
CREATE INDEX idx_user_accounts_email
    ON auth.user_accounts (lower(email));

-- The full handle "displayName#discriminator" must be unique. Display name is matched
-- case-insensitively so "Alice" and "alice" share the same discriminator pool.
CREATE UNIQUE INDEX uq_user_accounts_handle
    ON auth.user_accounts (lower(display_name), discriminator);
