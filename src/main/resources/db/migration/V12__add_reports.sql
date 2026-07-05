CREATE TABLE IF NOT EXISTS reports (
    id                    BIGSERIAL PRIMARY KEY,
    reporter_account_id   BIGINT NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    target_type           VARCHAR(20)  NOT NULL,
    target_id             BIGINT       NOT NULL,
    reason                VARCHAR(40)  NOT NULL,
    additional_note       TEXT,
    status                VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at            TIMESTAMP    NOT NULL DEFAULT NOW(),
    reviewed_at           TIMESTAMP,
    reviewed_by_account_id BIGINT REFERENCES accounts(id) ON DELETE SET NULL,

    CONSTRAINT uq_report_per_target UNIQUE (reporter_account_id, target_type, target_id)
);
