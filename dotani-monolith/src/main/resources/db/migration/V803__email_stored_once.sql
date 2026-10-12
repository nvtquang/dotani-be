-- An email is stored in exactly one place:
--   * member linked to a login account -> user_accounts.email (members.email is NULL)
--   * member without an account        -> members.email (contact email)
--
-- Linked members whose members.email differs from the login email (e.g. edited on the profile
-- without changing the login) keep the login email. The differing value is copied to
-- member_email_backup first so nothing is lost.

CREATE TABLE member_email_backup (
    member_id VARCHAR(36) NOT NULL,
    email VARCHAR(255) NOT NULL,
    account_email VARCHAR(255) NOT NULL,
    backed_up_at DATETIME(6) NOT NULL,
    PRIMARY KEY (member_id),
    CONSTRAINT fk_member_email_backup_member_id FOREIGN KEY (member_id) REFERENCES members (id)
);

INSERT INTO member_email_backup (member_id, email, account_email, backed_up_at)
SELECT m.id, m.email, u.email, UTC_TIMESTAMP(6)
FROM members m
JOIN user_accounts u ON u.id = m.user_id
WHERE m.email IS NOT NULL
  AND LOWER(TRIM(m.email)) <> LOWER(u.email);

UPDATE members m
JOIN user_accounts u ON u.id = m.user_id
SET m.email = NULL;
