ALTER TABLE user_accounts
    ADD COLUMN auth_provider VARCHAR(30) NOT NULL DEFAULT 'LOCAL';

ALTER TABLE user_accounts
    ADD COLUMN provider_subject VARCHAR(255) NULL;

ALTER TABLE user_accounts
    ADD CONSTRAINT uk_user_accounts_provider_subject UNIQUE (auth_provider, provider_subject);
