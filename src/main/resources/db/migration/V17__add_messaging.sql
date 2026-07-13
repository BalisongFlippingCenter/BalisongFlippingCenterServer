CREATE TABLE IF NOT EXISTS conversations (
    id                   BIGSERIAL PRIMARY KEY,
    participant_a_id     BIGINT NOT NULL,
    participant_b_id     BIGINT NOT NULL,
    last_message_at      TIMESTAMP,
    last_message_preview VARCHAR(100),
    unread_count_a       INT NOT NULL DEFAULT 0,
    unread_count_b       INT NOT NULL DEFAULT 0,
    deleted_by_a         BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_by_b         BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_conversation_pair UNIQUE (participant_a_id, participant_b_id)
);

CREATE INDEX IF NOT EXISTS idx_conversation_participant_a ON conversations (participant_a_id);
CREATE INDEX IF NOT EXISTS idx_conversation_participant_b ON conversations (participant_b_id);

CREATE TABLE IF NOT EXISTS messages (
    id              BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    sender_id       BIGINT NOT NULL,
    body            TEXT NOT NULL,
    sent_at         TIMESTAMP NOT NULL DEFAULT now(),
    read_at         TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_message_conversation_id ON messages (conversation_id);
