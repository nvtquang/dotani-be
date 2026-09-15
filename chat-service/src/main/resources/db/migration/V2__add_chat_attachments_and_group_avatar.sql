ALTER TABLE conversations ADD COLUMN avatar_url VARCHAR(500) NULL;

ALTER TABLE messages ADD COLUMN attachment_url VARCHAR(500) NULL;
ALTER TABLE messages ADD COLUMN attachment_name VARCHAR(255) NULL;
ALTER TABLE messages ADD COLUMN attachment_content_type VARCHAR(120) NULL;
ALTER TABLE messages ADD COLUMN attachment_size BIGINT NULL;
ALTER TABLE messages ADD COLUMN attachment_kind VARCHAR(20) NULL;
