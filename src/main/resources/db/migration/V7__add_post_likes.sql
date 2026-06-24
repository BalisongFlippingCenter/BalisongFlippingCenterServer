ALTER TABLE posts ADD COLUMN IF NOT EXISTS like_count INTEGER NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS post_likes (
    account_id BIGINT NOT NULL,
    post_id    BIGINT NOT NULL,
    PRIMARY KEY (account_id, post_id)
);

CREATE TABLE IF NOT EXISTS account_liked_posts (
    account_id BIGINT NOT NULL,
    post_id    BIGINT NOT NULL,
    PRIMARY KEY (account_id, post_id)
);
