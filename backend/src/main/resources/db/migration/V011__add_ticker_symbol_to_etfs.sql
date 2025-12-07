-- Add ticker_symbol column to etfs table for VanEck web importer
ALTER TABLE etfs ADD COLUMN ticker_symbol VARCHAR(50);

-- Add comment to describe the column
COMMENT ON COLUMN etfs.ticker_symbol IS 'Ticker symbol for VanEck web importer (e.g., TDIV, ESPO)';
