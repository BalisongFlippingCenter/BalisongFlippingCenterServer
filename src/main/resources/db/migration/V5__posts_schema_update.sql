-- Extend posts table with fields for all post types
ALTER TABLE posts ADD COLUMN description       TEXT;
ALTER TABLE posts ADD COLUMN reference_knife_id BIGINT;

-- Buy/Sell specific
ALTER TABLE posts ADD COLUMN mode              VARCHAR(20);
ALTER TABLE posts ADD COLUMN offering_knife_id BIGINT;

-- Trade specific
ALTER TABLE posts ADD COLUMN looking_for_text  TEXT;

-- Trick Tutorial / Combo specific
ALTER TABLE posts ADD COLUMN difficulty_tag    VARCHAR(50);

-- Media files attached to a post (images or videos)
CREATE TABLE post_media (
    post_id  BIGINT  NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    url      TEXT    NOT NULL,
    is_video BOOLEAN NOT NULL DEFAULT FALSE
);

-- Tags on Generic posts
CREATE TABLE post_generic_tags (
    post_id BIGINT      NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    tag     VARCHAR(50) NOT NULL
);

-- Technique tags on Trick Tutorial and Combo posts
CREATE TABLE post_technique_tags (
    post_id BIGINT      NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    tag     VARCHAR(50) NOT NULL
);
