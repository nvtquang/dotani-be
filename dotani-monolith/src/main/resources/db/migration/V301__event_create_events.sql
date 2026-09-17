CREATE TABLE events (
    id VARCHAR(36) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NULL,
    type VARCHAR(30) NOT NULL,
    location VARCHAR(255) NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    registration_deadline TIMESTAMP NULL,
    organization_id VARCHAR(36) NOT NULL,
    max_participants INT NULL,
    status VARCHAR(30) NOT NULL,
    created_by VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_events_type ON events (type);
CREATE INDEX idx_events_status ON events (status);
CREATE INDEX idx_events_organization_id ON events (organization_id);
CREATE INDEX idx_events_start_time ON events (start_time);
