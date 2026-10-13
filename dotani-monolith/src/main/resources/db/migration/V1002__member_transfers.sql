-- Chuyen sinh hoat: moving a member from one TDP to another. A request waits for a ward officer or an
-- officer of the receiving TDP; approved requests are the member's transfer history. Direct edits of a
-- member's TDP are recorded here too (already approved), so the history is complete.

CREATE TABLE member_transfers (
    id VARCHAR(36) NOT NULL,
    member_id VARCHAR(36) NOT NULL,
    from_organization_id VARCHAR(36) NOT NULL,
    to_organization_id VARCHAR(36) NOT NULL,
    reason VARCHAR(1000) NULL,
    status VARCHAR(20) NOT NULL,
    requested_by VARCHAR(36) NULL,
    requested_at DATETIME(6) NOT NULL,
    decided_by VARCHAR(36) NULL,
    decided_at DATETIME(6) NULL,
    decision_note VARCHAR(1000) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_member_transfers_member_id FOREIGN KEY (member_id) REFERENCES members (id),
    CONSTRAINT fk_member_transfers_from FOREIGN KEY (from_organization_id) REFERENCES organization_units (id),
    CONSTRAINT fk_member_transfers_to FOREIGN KEY (to_organization_id) REFERENCES organization_units (id),
    CONSTRAINT fk_member_transfers_requested_by FOREIGN KEY (requested_by) REFERENCES members (id),
    CONSTRAINT fk_member_transfers_decided_by FOREIGN KEY (decided_by) REFERENCES members (id)
);

CREATE INDEX idx_member_transfers_member ON member_transfers (member_id, requested_at);
CREATE INDEX idx_member_transfers_status ON member_transfers (status, requested_at);
