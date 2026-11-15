-- Apply once as postgres to the existing rbpo_lab4 database. Lab 4 data is retained.
BEGIN;

CREATE TABLE delivery.user_sessions (
    id UUID PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    device_id VARCHAR(255),
    access_token TEXT NOT NULL,
    refresh_token TEXT NOT NULL UNIQUE,
    access_token_expiry TIMESTAMPTZ NOT NULL,
    refresh_token_expiry TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL,
    CONSTRAINT user_sessions_status_valid CHECK (status IN ('ACTIVE', 'USED', 'REVOKED'))
);

CREATE INDEX user_sessions_username_idx ON delivery.user_sessions(username);
REVOKE ALL ON delivery.user_sessions FROM PUBLIC;
REVOKE ALL ON delivery.user_sessions FROM rbpo_lab4_app;
GRANT SELECT, INSERT, UPDATE ON delivery.user_sessions TO rbpo_lab4_app;

COMMIT;
