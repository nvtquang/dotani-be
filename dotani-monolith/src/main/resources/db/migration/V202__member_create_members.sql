CREATE TABLE members (
    id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NULL,
    full_name VARCHAR(255) NOT NULL,
    date_of_birth DATE NULL,
    gender VARCHAR(20) NULL,
    phone VARCHAR(30) NULL,
    email VARCHAR(255) NULL,
    address VARCHAR(500) NULL,
    avatar_url VARCHAR(1000) NULL,
    youth_union_join_date DATE NULL,
    member_status VARCHAR(30) NOT NULL,
    member_role VARCHAR(50) NOT NULL,
    organization_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_members_user_id UNIQUE (user_id),
    CONSTRAINT fk_members_organization_id FOREIGN KEY (organization_id) REFERENCES organization_units (id)
);

CREATE INDEX idx_members_organization_id ON members (organization_id);
CREATE INDEX idx_members_member_status ON members (member_status);
CREATE INDEX idx_members_full_name ON members (full_name);
