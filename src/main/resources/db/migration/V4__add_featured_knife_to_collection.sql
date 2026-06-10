-- Stores the ID of the knife the user has chosen as their featured knife.
-- Nullable — no featured knife selected by default.
ALTER TABLE collections ADD COLUMN featured_knife BIGINT;
