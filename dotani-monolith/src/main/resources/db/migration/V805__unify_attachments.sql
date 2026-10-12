-- One table for files attached to events, posts and chat messages (previously event_attachments,
-- post_images and five attachment_* columns on messages). Owners are polymorphic (owner_type, owner_id),
-- so owner_id has no foreign key; created_by references the uploading member.

CREATE TABLE attachments (
    id VARCHAR(36) NOT NULL,
    owner_type VARCHAR(20) NOT NULL,
    owner_id VARCHAR(36) NOT NULL,
    kind VARCHAR(20) NOT NULL,
    url VARCHAR(1000) NOT NULL,
    name VARCHAR(255) NULL,
    mime VARCHAR(120) NULL,
    size BIGINT NULL,
    created_by VARCHAR(36) NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_attachments_created_by FOREIGN KEY (created_by) REFERENCES members (id)
);

CREATE INDEX idx_attachments_owner ON attachments (owner_type, owner_id, created_at);

-- Ids of event and post attachments are kept: they appear in delete URLs.
INSERT INTO attachments (id, owner_type, owner_id, kind, url, name, mime, size, created_by, created_at)
SELECT id, 'EVENT', event_id, attachment_kind, file_url, file_name, NULL, NULL, NULL, created_at
FROM event_attachments;

INSERT INTO attachments (id, owner_type, owner_id, kind, url, name, mime, size, created_by, created_at)
SELECT id, 'POST', post_id, attachment_kind, image_url, file_name, NULL, NULL, NULL, created_at
FROM post_images;

INSERT INTO attachments (id, owner_type, owner_id, kind, url, name, mime, size, created_by, created_at)
SELECT UUID(), 'MESSAGE', id, COALESCE(attachment_kind, 'FILE'), attachment_url, attachment_name,
       attachment_content_type, attachment_size, sender_id, created_at
FROM messages
WHERE attachment_url IS NOT NULL;

DROP TABLE event_attachments;
DROP TABLE post_images;

ALTER TABLE messages
    DROP COLUMN attachment_url,
    DROP COLUMN attachment_name,
    DROP COLUMN attachment_content_type,
    DROP COLUMN attachment_size,
    DROP COLUMN attachment_kind;
