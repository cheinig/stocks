-- Add web_data_id column to etfs table for web-based importers
ALTER TABLE etfs ADD COLUMN web_data_id VARCHAR(100);

COMMENT ON COLUMN etfs.web_data_id IS 'Web data ID for constructing AJAX URLs (e.g., timestamp for iShares API)';
