CREATE TABLE makers (
    id                 BIGSERIAL PRIMARY KEY,
    slug               VARCHAR(255) NOT NULL UNIQUE,
    name               VARCHAR(255) NOT NULL,
    country            VARCHAR(255),
    known_for          TEXT,
    official_site_url  TEXT,
    last_checked_at    TIMESTAMP,
    content_hash       VARCHAR(255)
);

CREATE TABLE knives (
    id        BIGSERIAL PRIMARY KEY,
    slug      VARCHAR(255) NOT NULL UNIQUE,
    name      VARCHAR(255) NOT NULL,
    maker_id  BIGINT NOT NULL REFERENCES makers(id)
);

CREATE TABLE knife_versions (
    id                   BIGSERIAL PRIMARY KEY,
    knife_id             BIGINT NOT NULL REFERENCES knives(id) ON DELETE CASCADE,
    version_slug         VARCHAR(255) NOT NULL,
    version_label        VARCHAR(255) NOT NULL,
    discontinued         BOOLEAN NOT NULL DEFAULT FALSE,
    release_year         INTEGER,
    description          TEXT,
    overall_length       DOUBLE PRECISION,
    weight               DOUBLE PRECISION,
    pivot_system         VARCHAR(50),
    latch_type           VARCHAR(50),
    pin_system           VARCHAR(50),
    has_modular_balance  BOOLEAN NOT NULL DEFAULT FALSE,
    balance_value        VARCHAR(255),
    handle_construction  VARCHAR(50),
    handle_material      VARCHAR(50),
    handle_finish        VARCHAR(50),
    UNIQUE (knife_id, version_slug)
);

CREATE TABLE knife_variants (
    id                 BIGSERIAL PRIMARY KEY,
    knife_version_id   BIGINT NOT NULL REFERENCES knife_versions(id) ON DELETE CASCADE,
    variant_slug       VARCHAR(255) NOT NULL,
    type               VARCHAR(50) NOT NULL,
    label              VARCHAR(255) NOT NULL,
    msrp               DOUBLE PRECISION,
    blade_style        VARCHAR(50),
    blade_material     VARCHAR(50),
    blade_finish       VARCHAR(50),
    UNIQUE (knife_version_id, variant_slug)
);

CREATE TABLE where_to_find (
    id                 BIGSERIAL PRIMARY KEY,
    knife_version_id   BIGINT NOT NULL REFERENCES knife_versions(id) ON DELETE CASCADE,
    label              VARCHAR(255) NOT NULL,
    url                TEXT,
    type               VARCHAR(50) NOT NULL,
    note               TEXT,
    sort_order         INTEGER NOT NULL DEFAULT 0,
    last_checked_at    TIMESTAMP,
    content_hash       VARCHAR(255)
);
