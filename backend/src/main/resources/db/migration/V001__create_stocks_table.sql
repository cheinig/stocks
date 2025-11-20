-- Create stocks table
CREATE TABLE stocks (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    isin VARCHAR(12) NOT NULL UNIQUE,
    country VARCHAR(2) NOT NULL,
    sector VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version INTEGER NOT NULL DEFAULT 0
);

-- Create index on country for faster filtering
CREATE INDEX idx_stocks_country ON stocks(country);

-- Create index on sector for faster filtering
CREATE INDEX idx_stocks_sector ON stocks(sector);

-- Create index on name for search
CREATE INDEX idx_stocks_name ON stocks(name);

-- Comments
COMMENT ON TABLE stocks IS 'Base stock/asset information';
COMMENT ON COLUMN stocks.isin IS 'International Securities Identification Number - unique identifier';
COMMENT ON COLUMN stocks.country IS 'ISO 3166-1 alpha-2 country code';
COMMENT ON COLUMN stocks.sector IS 'Industry sector classification';
COMMENT ON COLUMN stocks.version IS 'Optimistic locking version';
