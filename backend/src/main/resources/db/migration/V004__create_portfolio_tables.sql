-- Create portfolios table
CREATE TABLE portfolios (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(100),
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version INTEGER NOT NULL DEFAULT 0
);

-- Create portfolio_positions table
CREATE TABLE portfolio_positions (
    id BIGSERIAL PRIMARY KEY,
    portfolio_id BIGINT NOT NULL,
    asset_type VARCHAR(20) NOT NULL,
    asset_id BIGINT NOT NULL,
    quantity DECIMAL(18, 6) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_position_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolios(id) ON DELETE CASCADE,
    CONSTRAINT chk_asset_type CHECK (asset_type IN ('STOCK', 'ETF')),
    CONSTRAINT chk_quantity_positive CHECK (quantity > 0),
    CONSTRAINT uk_portfolio_position UNIQUE (portfolio_id, asset_type, asset_id)
);

-- Create indexes
CREATE INDEX idx_portfolio_positions_portfolio ON portfolio_positions(portfolio_id);
CREATE INDEX idx_portfolio_positions_asset ON portfolio_positions(asset_type, asset_id);
CREATE INDEX idx_portfolios_user ON portfolios(user_id);

-- Comments
COMMENT ON TABLE portfolios IS 'User portfolios containing stocks and ETFs';
COMMENT ON COLUMN portfolios.user_id IS 'User identifier (optional for future multi-user support)';

COMMENT ON TABLE portfolio_positions IS 'Individual positions within a portfolio';
COMMENT ON COLUMN portfolio_positions.asset_type IS 'Type of asset: STOCK or ETF';
COMMENT ON COLUMN portfolio_positions.asset_id IS 'ID of the stock or ETF';
COMMENT ON COLUMN portfolio_positions.quantity IS 'Number of shares/units held';
