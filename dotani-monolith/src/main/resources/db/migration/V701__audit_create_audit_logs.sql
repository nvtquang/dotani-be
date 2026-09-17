CREATE TABLE audit_logs (
    id VARCHAR(36) NOT NULL,
    actor_user_id VARCHAR(36) NOT NULL,
    actor_role VARCHAR(50) NOT NULL,
    action VARCHAR(50) NOT NULL,
    resource_type VARCHAR(50) NOT NULL,
    resource_id VARCHAR(36) NOT NULL,
    organization_id VARCHAR(36) NULL,
    audit_timestamp TIMESTAMP NOT NULL,
    result VARCHAR(30) NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_audit_logs_timestamp ON audit_logs (audit_timestamp);
CREATE INDEX idx_audit_logs_action ON audit_logs (action);
CREATE INDEX idx_audit_logs_resource ON audit_logs (resource_type, resource_id);
CREATE INDEX idx_audit_logs_organization ON audit_logs (organization_id);
