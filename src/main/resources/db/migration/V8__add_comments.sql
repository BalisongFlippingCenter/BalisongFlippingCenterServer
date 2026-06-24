CREATE TABLE IF NOT EXISTS comments (
    id                BIGSERIAL PRIMARY KEY,
    post_id           BIGINT NOT NULL,
    account_id        BIGINT NOT NULL,
    content           TEXT NOT NULL,
    parent_comment_id BIGINT REFERENCES comments(id) ON DELETE CASCADE,
    like_count        INTEGER NOT NULL DEFAULT 0,
    reply_count       INTEGER NOT NULL DEFAULT 0,
    creation_date     TIMESTAMP NOT NULL DEFAULT NOW(),
    edited_date       TIMESTAMP
);

CREATE TABLE IF NOT EXISTS comment_likes (
    account_id BIGINT NOT NULL,
    comment_id BIGINT NOT NULL,
    PRIMARY KEY (account_id, comment_id)
);

CREATE TABLE IF NOT EXISTS account_liked_comments (
    account_id BIGINT NOT NULL,
    comment_id BIGINT NOT NULL,
    PRIMARY KEY (account_id, comment_id)
);
