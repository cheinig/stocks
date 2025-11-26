-- Create a demo portfolio with sample positions
-- This makes the application immediately usable after deployment

-- Insert demo portfolio
INSERT INTO portfolios (id, user_id, name, created_at, updated_at, version)
VALUES (1, 'demo', 'Demo Portfolio', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
ON CONFLICT (id) DO NOTHING;

-- Insert portfolio positions for stocks
INSERT INTO portfolio_positions (portfolio_id, asset_type, asset_id, quantity, created_at, updated_at)
SELECT
    1,
    'STOCK',
    s.id,
    p.quantity
FROM (
    SELECT 'US0378331005' as isin, 10.5 as quantity UNION ALL  -- Apple
    SELECT 'US5949181045', 15.0 UNION ALL                      -- Microsoft
    SELECT 'DE0007164600', 8.0 UNION ALL                       -- SAP
    SELECT 'CH0038863350', 5.0                                 -- Nestlé
) p
JOIN stocks s ON s.isin = p.isin
ON CONFLICT (portfolio_id, asset_type, asset_id) DO NOTHING;

-- Insert portfolio positions for ETFs
INSERT INTO portfolio_positions (portfolio_id, asset_type, asset_id, quantity, created_at, updated_at)
SELECT
    1,
    'ETF',
    e.id,
    p.quantity
FROM (
    SELECT 'IE00B4L5Y983' as isin, 25.0 as quantity UNION ALL  -- iShares Core MSCI World
    SELECT 'IE00B3RBWM25', 20.0                                -- Vanguard FTSE All-World
) p
JOIN etfs e ON e.isin = p.isin
ON CONFLICT (portfolio_id, asset_type, asset_id) DO NOTHING;

-- Reset sequence to start from 2 for user-created portfolios
SELECT setval('portfolios_id_seq', 1, true);
