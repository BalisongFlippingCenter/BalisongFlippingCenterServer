ALTER TABLE accounts
    ADD COLUMN banned          BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN ban_reason      TEXT,
    ADD COLUMN suspended_until TIMESTAMP,
    ADD COLUMN suspend_reason  TEXT,
    ADD COLUMN muted_until     TIMESTAMP,
    ADD COLUMN mute_reason     TEXT;
