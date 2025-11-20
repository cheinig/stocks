-- Create etfs table
CREATE TABLE etfs (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    isin VARCHAR(12) NOT NULL UNIQUE,
    importer_type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version INTEGER NOT NULL DEFAULT 0
);

-- Create index on importer_type
CREATE INDEX idx_etfs_importer_type ON etfs(importer_type);

-- Comments
COMMENT ON TABLE etfs IS 'ETF (Exchange Traded Fund) information';
COMMENT ON COLUMN etfs.isin IS 'International Securities Identification Number - unique identifier';
COMMENT ON COLUMN etfs.importer_type IS 'Type of importer to use for allocation files (e.g., GENERIC_CSV, GENERIC_EXCEL)';
COMMENT ON COLUMN etfs.version IS 'Optimistic locking version';
