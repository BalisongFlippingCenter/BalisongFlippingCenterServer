CREATE TABLE IF NOT EXISTS notifications (
    id                   BIGSERIAL PRIMARY KEY,
    recipient_account_id BIGINT      NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    actor_account_id     BIGINT      REFERENCES accounts(id) ON DELETE SET NULL,
    type                 VARCHAR(30) NOT NULL,
    target_type          VARCHAR(20) NOT NULL,
    target_id            BIGINT      NOT NULL,
    is_read              BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at           TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_recipient ON notifications(recipient_account_id, is_read, created_at DESC);
