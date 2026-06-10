-- Change score columns from VARCHAR to DOUBLE PRECISION
ALTER TABLE collection_knives
    ALTER COLUMN average_score    TYPE DOUBLE PRECISION USING CASE WHEN average_score    IS NULL OR average_score    = '' THEN 0.0 ELSE average_score::DOUBLE PRECISION    END,
    ALTER COLUMN quality_score    TYPE DOUBLE PRECISION USING CASE WHEN quality_score    IS NULL OR quality_score    = '' THEN 0.0 ELSE quality_score::DOUBLE PRECISION    END,
    ALTER COLUMN flipping_score   TYPE DOUBLE PRECISION USING CASE WHEN flipping_score   IS NULL OR flipping_score   = '' THEN 0.0 ELSE flipping_score::DOUBLE PRECISION   END,
    ALTER COLUMN feel_score       TYPE DOUBLE PRECISION USING CASE WHEN feel_score       IS NULL OR feel_score       = '' THEN 0.0 ELSE feel_score::DOUBLE PRECISION       END,
    ALTER COLUMN sound_score      TYPE DOUBLE PRECISION USING CASE WHEN sound_score      IS NULL OR sound_score      = '' THEN 0.0 ELSE sound_score::DOUBLE PRECISION      END,
    ALTER COLUMN durability_score TYPE DOUBLE PRECISION USING CASE WHEN durability_score IS NULL OR durability_score = '' THEN 0.0 ELSE durability_score::DOUBLE PRECISION END;

-- Extend image URL columns to accommodate full S3 URLs (can exceed 255 chars)
ALTER TABLE collection_knives              ALTER COLUMN cover_photo TYPE TEXT;
ALTER TABLE collection_knife_gallery_files ALTER COLUMN file_id     TYPE TEXT;
