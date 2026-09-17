CREATE TABLE posts (
    id VARCHAR(36) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    type VARCHAR(30) NOT NULL,
    organization_id VARCHAR(36) NOT NULL,
    author_id VARCHAR(36) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE post_images (
    id VARCHAR(36) NOT NULL,
    post_id VARCHAR(36) NOT NULL,
    image_url VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_post_images_post_id FOREIGN KEY (post_id) REFERENCES posts (id) ON DELETE CASCADE
);

CREATE INDEX idx_posts_type ON posts (type);
CREATE INDEX idx_posts_status ON posts (status);
CREATE INDEX idx_posts_organization_id ON posts (organization_id);
CREATE INDEX idx_posts_created_at ON posts (created_at);
CREATE INDEX idx_post_images_post_id ON post_images (post_id);
