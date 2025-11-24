-- Increase ISIN column length to support special identifiers like SONSTIGE
ALTER TABLE stocks ALTER COLUMN isin TYPE VARCHAR(13);

COMMENT ON COLUMN stocks.isin IS 'International Securities Identification Number (12 chars) or special identifier like SONSTIGE (13 chars)';
