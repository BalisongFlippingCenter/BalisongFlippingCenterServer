-- Add counts to accounts
ALTER TABLE accounts ADD COLUMN IF NOT EXISTS follower_count  INTEGER NOT NULL DEFAULT 0;
ALTER TABLE accounts ADD COLUMN IF NOT EXISTS following_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE accounts ADD COLUMN IF NOT EXISTS post_count      INTEGER NOT NULL DEFAULT 0;

-- Backfill post count from existing posts
UPDATE accounts a
SET post_count = (
    SELECT COUNT(*) FROM posts p WHERE p.account_id = a.id::TEXT
);

-- Follows table
CREATE TABLE IF NOT EXISTS follows (
    follower_id  BIGINT NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    following_id BIGINT NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    PRIMARY KEY (follower_id, following_id)
);
