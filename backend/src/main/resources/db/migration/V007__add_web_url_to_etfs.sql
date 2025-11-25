-- Add web_url column to etfs table for web-based importers
ALTER TABLE etfs ADD COLUMN web_url VARCHAR(500);

-- Add comment to describe the column
COMMENT ON COLUMN etfs.web_url IS 'Base URL for web-based importers (e.g., iShares holdings page)';
