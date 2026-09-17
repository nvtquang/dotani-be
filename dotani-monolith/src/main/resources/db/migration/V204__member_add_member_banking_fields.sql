ALTER TABLE members
    ADD COLUMN bank_name VARCHAR(255) NULL;

ALTER TABLE members
    ADD COLUMN bank_code VARCHAR(50) NULL;

ALTER TABLE members
    ADD COLUMN account_number VARCHAR(50) NULL;

ALTER TABLE members
    ADD COLUMN account_holder_name VARCHAR(255) NULL;

ALTER TABLE members
    ADD COLUMN bank_qr_image_url VARCHAR(1000) NULL;
