ALTER TABLE knife_variants
    ADD COLUMN trainer_blade VARCHAR(50);

UPDATE knife_variants SET trainer_blade = 'STANDARD' WHERE type = 'TRAINER';
