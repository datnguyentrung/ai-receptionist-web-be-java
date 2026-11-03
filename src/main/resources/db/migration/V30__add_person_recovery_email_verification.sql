ALTER TABLE core.person
    ADD COLUMN recovery_email_verified_value VARCHAR(100),
    ADD COLUMN recovery_email_verified_at TIMESTAMP;
