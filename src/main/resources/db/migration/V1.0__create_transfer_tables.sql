-- V1.0__create_transfer_tables.sql
-- Initial schema for Transfer Orchestration Service

-- Table: transfer_requests
-- Stores the main transfer lifecycle
CREATE TABLE transfer_requests (
    id BIGSERIAL PRIMARY KEY,
    consumer_id VARCHAR(100) NOT NULL,
    provider_id VARCHAR(100) NOT NULL,
    asset_id VARCHAR(100) NOT NULL,
    data_type INTEGER NOT NULL,
    status INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_transfer_requests_consumer ON transfer_requests(consumer_id);
CREATE INDEX idx_transfer_requests_provider ON transfer_requests(provider_id);
CREATE INDEX idx_transfer_requests_status ON transfer_requests(status);
CREATE INDEX idx_transfer_requests_created_at ON transfer_requests(created_at);

-- Table: transfer_state_history
-- Tracks state transitions for resilience and audit
CREATE TABLE transfer_state_history (
    id BIGSERIAL PRIMARY KEY,
    transfer_id BIGINT NOT NULL,
    from_state INTEGER,
    to_state INTEGER NOT NULL,
    changed_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_transfer_state_history_transfer FOREIGN KEY (transfer_id) REFERENCES transfer_requests(id) ON DELETE CASCADE
);

CREATE INDEX idx_transfer_state_history_transfer ON transfer_state_history(transfer_id);
CREATE INDEX idx_transfer_state_history_changed_at ON transfer_state_history(changed_at);

-- Table: audit_logs
-- Immutable audit trail for compliance
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    transfer_id BIGINT,
    event_type INTEGER NOT NULL,
    actor VARCHAR(100) NOT NULL,
    message TEXT,
    metadata JSONB,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_audit_logs_transfer FOREIGN KEY (transfer_id) REFERENCES transfer_requests(id) ON DELETE SET NULL
);

CREATE INDEX idx_audit_logs_transfer ON audit_logs(transfer_id);
CREATE INDEX idx_audit_logs_event_type ON audit_logs(event_type);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);
CREATE INDEX idx_audit_logs_metadata ON audit_logs USING GIN(metadata);