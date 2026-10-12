-- Audit logs record what changed (details: {"field": {"before": ..., "after": ...}}) and where the request
-- came from. Existing rows keep NULL: their before/after values were never captured.
ALTER TABLE audit_logs
    ADD COLUMN details JSON NULL,
    ADD COLUMN ip VARCHAR(45) NULL,
    ADD COLUMN user_agent VARCHAR(500) NULL;
