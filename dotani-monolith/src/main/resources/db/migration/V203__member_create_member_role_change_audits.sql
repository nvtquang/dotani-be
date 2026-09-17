CREATE TABLE member_role_change_audits (
    id VARCHAR(36) NOT NULL,
    member_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NULL,
    old_role VARCHAR(50) NOT NULL,
    new_role VARCHAR(50) NOT NULL,
    actor_user_id VARCHAR(36) NOT NULL,
    actor_member_id VARCHAR(36) NULL,
    created_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_member_role_change_audits_member_id FOREIGN KEY (member_id) REFERENCES members (id)
);

CREATE INDEX idx_member_role_change_audits_member_id ON member_role_change_audits (member_id);
CREATE INDEX idx_member_role_change_audits_actor_user_id ON member_role_change_audits (actor_user_id);
