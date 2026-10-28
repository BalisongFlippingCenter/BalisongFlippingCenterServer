ALTER TABLE email_verification_tokens
    ADD COLUMN failed_attempts INTEGER NOT NULL DEFAULT 0;
