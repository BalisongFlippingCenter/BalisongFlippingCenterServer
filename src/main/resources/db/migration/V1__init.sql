-- accounts table (SINGLE_TABLE inheritance: USER, ACCOUNT)
CREATE TABLE accounts (
    id                   BIGSERIAL PRIMARY KEY,
    dtype                VARCHAR(31)  NOT NULL,
    email                VARCHAR(255) NOT NULL UNIQUE,
    role                 VARCHAR(50),
    password             VARCHAR(255),
    account_creation_date TIMESTAMP,
    last_login           TIMESTAMP,
    email_verified       BOOLEAN DEFAULT FALSE,

    -- User-specific columns (NULL for non-User rows)
    display_name         VARCHAR(255),
    identifier_code      VARCHAR(10),
    profile_img          VARCHAR(255),
    banner_img           VARCHAR(255),
    collection_id        BIGINT,
    facebook_link        VARCHAR(255),
    twitter_link         VARCHAR(255),
    instagram_link       VARCHAR(255),
    youtube_link         VARCHAR(255),
    discord_link         VARCHAR(255),
    reddit_link          VARCHAR(255),
    personal_email_link  VARCHAR(255),
    personal_website_link VARCHAR(255)
);

CREATE TABLE collections (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT UNIQUE,
    banner_img VARCHAR(255)
);

CREATE TABLE collection_knives (
    id                  BIGSERIAL PRIMARY KEY,
    collection_id       BIGINT,
    display_name        VARCHAR(255),
    knife_maker         VARCHAR(255),
    base_knife_model    VARCHAR(255),
    knife_type          VARCHAR(50),
    aqquired_date       VARCHAR(255),
    creation_date       TIMESTAMP,
    is_favorite_knife   BOOLEAN DEFAULT FALSE,
    is_favorite_flipper BOOLEAN DEFAULT FALSE,
    cover_photo         VARCHAR(255),
    msrp                DOUBLE PRECISION,
    overall_length      DOUBLE PRECISION,
    weight              DOUBLE PRECISION,
    pivot_system        VARCHAR(50),
    pin_system          VARCHAR(50),
    latch_type          VARCHAR(50),
    has_modular_balance BOOLEAN DEFAULT FALSE,
    balance_value       VARCHAR(255),
    blade_style         VARCHAR(50),
    blade_finish        VARCHAR(50),
    blade_material      VARCHAR(50),
    handle_construction VARCHAR(50),
    handle_finish       VARCHAR(50),
    handle_material     VARCHAR(50),
    average_score       VARCHAR(50),
    quality_score       VARCHAR(50),
    flipping_score      VARCHAR(50),
    feel_score          VARCHAR(50),
    sound_score         VARCHAR(50),
    durability_score    VARCHAR(50),
    has_been_sold       BOOLEAN DEFAULT FALSE,
    want_to_trade       BOOLEAN DEFAULT FALSE,
    up_for_sale         BOOLEAN DEFAULT FALSE,
    no_longer_have      BOOLEAN DEFAULT FALSE
);

CREATE TABLE collection_knife_gallery_files (
    collection_knife_id BIGINT NOT NULL REFERENCES collection_knives(id),
    post_id             VARCHAR(255),
    file_id             VARCHAR(255)
);

CREATE TABLE refresh_tokens (
    id         BIGSERIAL PRIMARY KEY,
    token      VARCHAR(255) NOT NULL UNIQUE,
    expiration TIMESTAMP    NOT NULL,
    owner_id   BIGINT       NOT NULL REFERENCES accounts(id)
);

CREATE TABLE email_verification_tokens (
    id         BIGSERIAL PRIMARY KEY,
    token      VARCHAR(255) NOT NULL,
    expiration TIMESTAMP    NOT NULL,
    owner_id   BIGINT       NOT NULL REFERENCES accounts(id)
);

CREATE TABLE posts (
    id            BIGSERIAL PRIMARY KEY,
    account_id    VARCHAR(255),
    caption       TEXT,
    post_type     VARCHAR(50),
    creation_date TIMESTAMP
);
