CREATE TABLE notifications (
    id VARCHAR(36) NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    reference_type VARCHAR(50) NULL,
    reference_id VARCHAR(36) NULL,
    created_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE user_notifications (
    id VARCHAR(36) NOT NULL,
    notification_id VARCHAR(36) NOT NULL,
    member_id VARCHAR(36) NOT NULL,
    read_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_notifications_notification_member UNIQUE (notification_id, member_id),
    CONSTRAINT fk_user_notifications_notification_id FOREIGN KEY (notification_id) REFERENCES notifications (id) ON DELETE CASCADE
);

CREATE INDEX idx_notifications_created_at ON notifications (created_at);
CREATE INDEX idx_user_notifications_member_id ON user_notifications (member_id);
CREATE INDEX idx_user_notifications_member_read_at ON user_notifications (member_id, read_at);
