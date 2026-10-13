-- Khao sat / bieu quyet. Eligible voters are the active members of the poll's organization (a ward
-- reaches all of its TDPs). poll_ballots records who voted (turnout, one vote each); poll_answers holds
-- the chosen options and, for secret ballots, does not store the member.

CREATE TABLE polls (
    id VARCHAR(36) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NULL,
    organization_id VARCHAR(36) NOT NULL,
    event_id VARCHAR(36) NULL,
    anonymous BOOLEAN NOT NULL,
    max_choices INT NOT NULL,
    results_visibility VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    closes_at DATETIME(6) NULL,
    opened_at DATETIME(6) NULL,
    closed_at DATETIME(6) NULL,
    created_by VARCHAR(36) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_polls_organization_id FOREIGN KEY (organization_id) REFERENCES organization_units (id),
    CONSTRAINT fk_polls_event_id FOREIGN KEY (event_id) REFERENCES events (id),
    CONSTRAINT fk_polls_created_by FOREIGN KEY (created_by) REFERENCES members (id)
);

CREATE INDEX idx_polls_organization ON polls (organization_id, created_at);
CREATE INDEX idx_polls_status_closes ON polls (status, closes_at);

CREATE TABLE poll_options (
    id VARCHAR(36) NOT NULL,
    poll_id VARCHAR(36) NOT NULL,
    label VARCHAR(255) NOT NULL,
    position INT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_poll_options_poll_id FOREIGN KEY (poll_id) REFERENCES polls (id) ON DELETE CASCADE
);

CREATE INDEX idx_poll_options_poll ON poll_options (poll_id, position);

CREATE TABLE poll_ballots (
    poll_id VARCHAR(36) NOT NULL,
    member_id VARCHAR(36) NOT NULL,
    voted_at DATETIME(6) NOT NULL,
    PRIMARY KEY (poll_id, member_id),
    CONSTRAINT fk_poll_ballots_poll_id FOREIGN KEY (poll_id) REFERENCES polls (id) ON DELETE CASCADE,
    CONSTRAINT fk_poll_ballots_member_id FOREIGN KEY (member_id) REFERENCES members (id)
);

CREATE TABLE poll_answers (
    id VARCHAR(36) NOT NULL,
    poll_id VARCHAR(36) NOT NULL,
    option_id VARCHAR(36) NOT NULL,
    member_id VARCHAR(36) NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_poll_answers_poll_id FOREIGN KEY (poll_id) REFERENCES polls (id) ON DELETE CASCADE,
    CONSTRAINT fk_poll_answers_option_id FOREIGN KEY (option_id) REFERENCES poll_options (id) ON DELETE CASCADE,
    CONSTRAINT fk_poll_answers_member_id FOREIGN KEY (member_id) REFERENCES members (id)
);

CREATE INDEX idx_poll_answers_poll_option ON poll_answers (poll_id, option_id);
