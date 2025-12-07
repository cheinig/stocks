-- Add logo columns to stocks table
ALTER TABLE stocks
    ADD COLUMN logo BYTEA,
    ADD COLUMN logo_content_type VARCHAR(50);

-- Add logo columns to etfs table
ALTER TABLE etfs
    ADD COLUMN logo BYTEA,
    ADD COLUMN logo_content_type VARCHAR(50);
