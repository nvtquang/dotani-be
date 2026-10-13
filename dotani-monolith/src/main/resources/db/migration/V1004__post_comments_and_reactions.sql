-- Comments and reactions on posts. One reaction per member and post; comments are soft-deleted.

CREATE TABLE post_comments (
    id VARCHAR(36) NOT NULL,
    post_id VARCHAR(36) NOT NULL,
    member_id VARCHAR(36) NOT NULL,
    content TEXT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_post_comments_post_id FOREIGN KEY (post_id) REFERENCES posts (id),
    CONSTRAINT fk_post_comments_member_id FOREIGN KEY (member_id) REFERENCES members (id)
);

CREATE INDEX idx_post_comments_post ON post_comments (post_id, created_at);

CREATE TABLE post_reactions (
    post_id VARCHAR(36) NOT NULL,
    member_id VARCHAR(36) NOT NULL,
    reaction VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (post_id, member_id),
    CONSTRAINT fk_post_reactions_post_id FOREIGN KEY (post_id) REFERENCES posts (id),
    CONSTRAINT fk_post_reactions_member_id FOREIGN KEY (member_id) REFERENCES members (id)
);
