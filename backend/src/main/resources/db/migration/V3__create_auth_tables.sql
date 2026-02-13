-- Drop api_key column from clients (moving to separate table)
ALTER TABLE clients DROP COLUMN IF EXISTS api_key;

-- API Keys table (many keys per client)
CREATE TABLE api_keys (
    api_key_id BIGSERIAL PRIMARY KEY,
    client_id BIGINT NOT NULL,
    key_value VARCHAR(74) NOT NULL UNIQUE,  -- sk_live_<64 hex chars>
    name VARCHAR(100) DEFAULT 'Default',
    is_active BOOLEAN DEFAULT TRUE,
    last_used_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    revoked_at TIMESTAMP,

    CONSTRAINT fk_api_keys_client FOREIGN KEY (client_id) REFERENCES clients(client_id) ON DELETE CASCADE
);

-- Refresh Tokens table (for JWT authentication)
CREATE TABLE refresh_tokens (
    refresh_token_id BIGSERIAL PRIMARY KEY,
    client_id BIGINT NOT NULL,
    token_value VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    is_revoked BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    revoked_at TIMESTAMP,

    CONSTRAINT fk_refresh_tokens_client FOREIGN KEY (client_id) REFERENCES clients(client_id) ON DELETE CASCADE
);

-- Indexes for performance
CREATE INDEX idx_api_keys_client ON api_keys(client_id);
CREATE INDEX idx_api_keys_value ON api_keys(key_value);
CREATE INDEX idx_api_keys_active ON api_keys(is_active);

CREATE INDEX idx_refresh_tokens_client ON refresh_tokens(client_id);
CREATE INDEX idx_refresh_tokens_value ON refresh_tokens(token_value);
CREATE INDEX idx_refresh_tokens_expires ON refresh_tokens(expires_at);
CREATE INDEX idx_refresh_tokens_active ON refresh_tokens(is_revoked);

-- Comments for clarity
COMMENT ON TABLE clients IS 'API users/accounts (humans with credentials)';
COMMENT ON TABLE api_keys IS 'API Keys for programmatic access (scripts, notebooks, CI/CD)';
COMMENT ON TABLE refresh_tokens IS 'Refresh tokens for JWT authentication (web dashboard, mobile apps)';
