ALTER TABLE orchestrator.audit_logs
    ADD COLUMN consumer_id VARCHAR(255);

CREATE INDEX idx_audit_event_consumer_id ON orchestrator.audit_logs(consumer_id);