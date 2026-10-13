-- Doan phi: a collection period (one amount per member) and the payments recorded against it.
-- A period belongs to the ward (every member of the ward pays) or to one TDP (its members pay).
-- The collector is the officer whose bank details and QR are shown to members who transfer.

CREATE TABLE fee_periods (
    id VARCHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT NULL,
    organization_id VARCHAR(36) NOT NULL,
    amount BIGINT NOT NULL,
    due_date DATE NULL,
    collector_member_id VARCHAR(36) NULL,
    status VARCHAR(20) NOT NULL,
    last_reminded_at DATETIME(6) NULL,
    created_by VARCHAR(36) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_fee_periods_organization_id FOREIGN KEY (organization_id) REFERENCES organization_units (id),
    CONSTRAINT fk_fee_periods_collector_member_id FOREIGN KEY (collector_member_id) REFERENCES members (id),
    CONSTRAINT fk_fee_periods_created_by FOREIGN KEY (created_by) REFERENCES members (id)
);

CREATE INDEX idx_fee_periods_organization ON fee_periods (organization_id, created_at);

CREATE TABLE fee_payments (
    id VARCHAR(36) NOT NULL,
    period_id VARCHAR(36) NOT NULL,
    member_id VARCHAR(36) NOT NULL,
    amount BIGINT NOT NULL,
    method VARCHAR(20) NOT NULL,
    paid_at DATETIME(6) NOT NULL,
    note VARCHAR(500) NULL,
    recorded_by VARCHAR(36) NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_fee_payments_period_member UNIQUE (period_id, member_id),
    CONSTRAINT fk_fee_payments_period_id FOREIGN KEY (period_id) REFERENCES fee_periods (id) ON DELETE CASCADE,
    CONSTRAINT fk_fee_payments_member_id FOREIGN KEY (member_id) REFERENCES members (id),
    CONSTRAINT fk_fee_payments_recorded_by FOREIGN KEY (recorded_by) REFERENCES members (id)
);

CREATE INDEX idx_fee_payments_member ON fee_payments (member_id);
