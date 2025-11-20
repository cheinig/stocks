-- Create etf_allocations table
CREATE TABLE etf_allocations (
    id BIGSERIAL PRIMARY KEY,
    etf_id BIGINT NOT NULL,
    stock_id BIGINT NOT NULL,
    percentage DECIMAL(10, 6) NOT NULL,
    upload_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    allocation_version INTEGER NOT NULL DEFAULT 1,
    CONSTRAINT fk_allocation_etf FOREIGN KEY (etf_id) REFERENCES etfs(id) ON DELETE CASCADE,
    CONSTRAINT fk_allocation_stock FOREIGN KEY (stock_id) REFERENCES stocks(id) ON DELETE CASCADE,
    CONSTRAINT chk_percentage_positive CHECK (percentage > 0),
    CONSTRAINT chk_percentage_max CHECK (percentage <= 100)
);

-- Create composite index for efficient queries
CREATE INDEX idx_etf_allocations_etf_version ON etf_allocations(etf_id, allocation_version DESC);

-- Create index on stock_id for reverse lookups
CREATE INDEX idx_etf_allocations_stock ON etf_allocations(stock_id);

-- Comments
COMMENT ON TABLE etf_allocations IS 'Historical allocation data for ETFs showing which stocks they contain';
COMMENT ON COLUMN etf_allocations.percentage IS 'Percentage of ETF allocated to this stock (0-100)';
COMMENT ON COLUMN etf_allocations.allocation_version IS 'Version number for tracking allocation changes over time';
COMMENT ON COLUMN etf_allocations.upload_date IS 'When this allocation data was uploaded';
