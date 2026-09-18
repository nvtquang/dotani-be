CREATE TABLE event_attachments (
    id VARCHAR(36) PRIMARY KEY,
    event_id VARCHAR(36) NOT NULL,
    file_url VARCHAR(1000) NOT NULL,
    attachment_kind VARCHAR(20) NOT NULL,
    file_name VARCHAR(255),
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_event_attachments_event
        FOREIGN KEY (event_id) REFERENCES events(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_event_attachments_event_id ON event_attachments(event_id);
