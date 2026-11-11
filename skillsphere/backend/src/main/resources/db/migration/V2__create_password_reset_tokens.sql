CREATE TABLE password_reset_tokens (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash  VARCHAR(64)  NOT NULL,      -- SHA-256 of the emailed token; the raw token is never stored
    expires_at  TIMESTAMPTZ  NOT NULL,
    used_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_prt_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_prt_user ON password_reset_tokens (user_id);
