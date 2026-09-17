CREATE TABLE organization_units (
    id VARCHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(100) NOT NULL,
    type VARCHAR(50) NOT NULL,
    parent_id VARCHAR(36) NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_organization_units_code UNIQUE (code),
    CONSTRAINT fk_organization_units_parent_id FOREIGN KEY (parent_id) REFERENCES organization_units (id)
);
