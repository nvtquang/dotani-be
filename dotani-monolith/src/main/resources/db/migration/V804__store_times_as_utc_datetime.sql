-- All instants are now stored as DATETIME(6) holding UTC (Hibernate writes with hibernate.jdbc.time_zone=UTC).
--
-- Before this migration the app wrote Vietnam wall-clock time: TIMESTAMP columns were written and are read
-- back through the session time zone, and DATETIME columns received LocalDateTime.now() of a JVM running in
-- Vietnam. So each value is first read as it was written (MODIFY keeps the wall-clock value shown by the
-- session), then shifted from the legacy zone to UTC.
--
-- ${legacy_time_zone} defaults to +07:00 (spring.flyway.placeholders.legacy_time_zone / LEGACY_TIME_ZONE).
-- Data written by an app container running in UTC is the exception: set LEGACY_TIME_ZONE=+00:00 there.

-- auth
ALTER TABLE user_accounts
    MODIFY created_at DATETIME(6) NOT NULL,
    MODIFY updated_at DATETIME(6) NOT NULL;
UPDATE user_accounts
SET created_at = CONVERT_TZ(created_at, '${legacy_time_zone}', '+00:00'),
    updated_at = CONVERT_TZ(updated_at, '${legacy_time_zone}', '+00:00');

ALTER TABLE refresh_tokens
    MODIFY expires_at DATETIME(6) NOT NULL,
    MODIFY created_at DATETIME(6) NOT NULL;
UPDATE refresh_tokens
SET expires_at = CONVERT_TZ(expires_at, '${legacy_time_zone}', '+00:00'),
    created_at = CONVERT_TZ(created_at, '${legacy_time_zone}', '+00:00');

-- member
ALTER TABLE organization_units
    MODIFY created_at DATETIME(6) NOT NULL,
    MODIFY updated_at DATETIME(6) NOT NULL;
UPDATE organization_units
SET created_at = CONVERT_TZ(created_at, '${legacy_time_zone}', '+00:00'),
    updated_at = CONVERT_TZ(updated_at, '${legacy_time_zone}', '+00:00');

ALTER TABLE members
    MODIFY created_at DATETIME(6) NOT NULL,
    MODIFY updated_at DATETIME(6) NOT NULL;
UPDATE members
SET created_at = CONVERT_TZ(created_at, '${legacy_time_zone}', '+00:00'),
    updated_at = CONVERT_TZ(updated_at, '${legacy_time_zone}', '+00:00');

ALTER TABLE member_role_change_audits
    MODIFY created_at DATETIME(6) NOT NULL;
UPDATE member_role_change_audits
SET created_at = CONVERT_TZ(created_at, '${legacy_time_zone}', '+00:00');

-- event
ALTER TABLE events
    MODIFY start_time DATETIME(6) NOT NULL,
    MODIFY end_time DATETIME(6) NOT NULL,
    MODIFY registration_deadline DATETIME(6) NULL,
    MODIFY created_at DATETIME(6) NOT NULL,
    MODIFY updated_at DATETIME(6) NOT NULL;
UPDATE events
SET start_time = CONVERT_TZ(start_time, '${legacy_time_zone}', '+00:00'),
    end_time = CONVERT_TZ(end_time, '${legacy_time_zone}', '+00:00'),
    registration_deadline = CONVERT_TZ(registration_deadline, '${legacy_time_zone}', '+00:00'),
    created_at = CONVERT_TZ(created_at, '${legacy_time_zone}', '+00:00'),
    updated_at = CONVERT_TZ(updated_at, '${legacy_time_zone}', '+00:00');

ALTER TABLE event_participations
    MODIFY created_at DATETIME(6) NOT NULL,
    MODIFY updated_at DATETIME(6) NOT NULL;
UPDATE event_participations
SET created_at = CONVERT_TZ(created_at, '${legacy_time_zone}', '+00:00'),
    updated_at = CONVERT_TZ(updated_at, '${legacy_time_zone}', '+00:00');

ALTER TABLE event_attachments
    MODIFY created_at DATETIME(6) NOT NULL;
UPDATE event_attachments
SET created_at = CONVERT_TZ(created_at, '${legacy_time_zone}', '+00:00');

-- content
ALTER TABLE posts
    MODIFY created_at DATETIME(6) NOT NULL,
    MODIFY updated_at DATETIME(6) NOT NULL;
UPDATE posts
SET created_at = CONVERT_TZ(created_at, '${legacy_time_zone}', '+00:00'),
    updated_at = CONVERT_TZ(updated_at, '${legacy_time_zone}', '+00:00');

ALTER TABLE post_images
    MODIFY created_at DATETIME(6) NOT NULL;
UPDATE post_images
SET created_at = CONVERT_TZ(created_at, '${legacy_time_zone}', '+00:00');

-- chat
ALTER TABLE conversations
    MODIFY created_at DATETIME(6) NOT NULL,
    MODIFY updated_at DATETIME(6) NOT NULL;
UPDATE conversations
SET created_at = CONVERT_TZ(created_at, '${legacy_time_zone}', '+00:00'),
    updated_at = CONVERT_TZ(updated_at, '${legacy_time_zone}', '+00:00');

ALTER TABLE conversation_members
    MODIFY created_at DATETIME(6) NOT NULL,
    MODIFY last_read_at DATETIME(6) NULL;
UPDATE conversation_members
SET created_at = CONVERT_TZ(created_at, '${legacy_time_zone}', '+00:00'),
    last_read_at = CONVERT_TZ(last_read_at, '${legacy_time_zone}', '+00:00');

ALTER TABLE messages
    MODIFY created_at DATETIME(6) NOT NULL;
UPDATE messages
SET created_at = CONVERT_TZ(created_at, '${legacy_time_zone}', '+00:00');

-- notification
ALTER TABLE notifications
    MODIFY created_at DATETIME(6) NOT NULL;
UPDATE notifications
SET created_at = CONVERT_TZ(created_at, '${legacy_time_zone}', '+00:00');

ALTER TABLE user_notifications
    MODIFY read_at DATETIME(6) NULL,
    MODIFY created_at DATETIME(6) NOT NULL;
UPDATE user_notifications
SET read_at = CONVERT_TZ(read_at, '${legacy_time_zone}', '+00:00'),
    created_at = CONVERT_TZ(created_at, '${legacy_time_zone}', '+00:00');

-- audit
ALTER TABLE audit_logs
    MODIFY audit_timestamp DATETIME(6) NOT NULL;
UPDATE audit_logs
SET audit_timestamp = CONVERT_TZ(audit_timestamp, '${legacy_time_zone}', '+00:00');
