-- Ho so thi dua: commendations and disciplinary measures (each backed by a decision), and the yearly
-- rating of each member (one per member and year).

CREATE TABLE member_awards (
    id VARCHAR(36) NOT NULL,
    member_id VARCHAR(36) NOT NULL,
    form VARCHAR(40) NOT NULL,
    title VARCHAR(255) NOT NULL,
    reason TEXT NULL,
    decision_number VARCHAR(100) NULL,
    decision_date DATE NOT NULL,
    issuer VARCHAR(255) NULL,
    created_by VARCHAR(36) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_member_awards_member_id FOREIGN KEY (member_id) REFERENCES members (id),
    CONSTRAINT fk_member_awards_created_by FOREIGN KEY (created_by) REFERENCES members (id)
);

CREATE INDEX idx_member_awards_member ON member_awards (member_id, decision_date);

CREATE TABLE member_ratings (
    id VARCHAR(36) NOT NULL,
    member_id VARCHAR(36) NOT NULL,
    rating_year INT NOT NULL,
    rating VARCHAR(30) NOT NULL,
    note VARCHAR(1000) NULL,
    rated_by VARCHAR(36) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_member_ratings_member_year UNIQUE (member_id, rating_year),
    CONSTRAINT fk_member_ratings_member_id FOREIGN KEY (member_id) REFERENCES members (id),
    CONSTRAINT fk_member_ratings_rated_by FOREIGN KEY (rated_by) REFERENCES members (id)
);

CREATE INDEX idx_member_ratings_year ON member_ratings (rating_year, rating);
