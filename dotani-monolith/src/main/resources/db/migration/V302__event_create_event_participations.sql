CREATE TABLE event_participations (
    id VARCHAR(36) NOT NULL,
    event_id VARCHAR(36) NOT NULL,
    member_id VARCHAR(36) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_event_participations_event_member UNIQUE (event_id, member_id),
    CONSTRAINT fk_event_participations_event_id FOREIGN KEY (event_id) REFERENCES events (id) ON DELETE CASCADE
);

CREATE INDEX idx_event_participations_member_id ON event_participations (member_id);
CREATE INDEX idx_event_participations_event_status ON event_participations (event_id, status);
