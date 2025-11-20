-- Seed script for example countries and sectors
-- This script is repeatable and will be executed after all versioned migrations

-- Insert example stocks from various countries and sectors
INSERT INTO stocks (name, isin, country, sector) VALUES
    ('Apple Inc.', 'US0378331005', 'US', 'Technology'),
    ('Microsoft Corp.', 'US5949181045', 'US', 'Technology'),
    ('Amazon.com Inc.', 'US0231351067', 'US', 'Consumer Cyclical'),
    ('Alphabet Inc.', 'US02079K3059', 'US', 'Technology'),
    ('Tesla Inc.', 'US88160R1014', 'US', 'Consumer Cyclical'),
    ('SAP SE', 'DE0007164600', 'DE', 'Technology'),
    ('Siemens AG', 'DE0007236101', 'DE', 'Industrials'),
    ('ASML Holding', 'NL0010273215', 'NL', 'Technology'),
    ('Nestlé S.A.', 'CH0038863350', 'CH', 'Consumer Defensive'),
    ('Roche Holding AG', 'CH0012032048', 'CH', 'Healthcare'),
    ('LVMH', 'FR0000121014', 'FR', 'Consumer Cyclical'),
    ('TotalEnergies SE', 'FR0000120271', 'FR', 'Energy'),
    ('HSBC Holdings', 'GB0005405286', 'GB', 'Financial Services'),
    ('Unilever PLC', 'GB00B10RZP78', 'GB', 'Consumer Defensive'),
    ('Toyota Motor Corp.', 'JP3633400001', 'JP', 'Consumer Cyclical'),
    ('Sony Group Corp.', 'JP3435000009', 'JP', 'Technology')
ON CONFLICT (isin) DO NOTHING;

-- Insert example ETFs
INSERT INTO etfs (name, isin, importer_type) VALUES
    ('iShares Core MSCI World', 'IE00B4L5Y983', 'GENERIC_CSV'),
    ('Vanguard FTSE All-World', 'IE00B3RBWM25', 'GENERIC_CSV'),
    ('iShares Core S&P 500', 'IE00B5BMR087', 'GENERIC_CSV')
ON CONFLICT (isin) DO NOTHING;

-- Insert example ETF allocations for iShares Core MSCI World
-- This is simplified - in reality ETFs have hundreds of holdings
INSERT INTO etf_allocations (etf_id, stock_id, percentage, allocation_version)
SELECT
    (SELECT id FROM etfs WHERE isin = 'IE00B4L5Y983'),
    s.id,
    p.percentage,
    1
FROM (
    SELECT 'US0378331005' as isin, 4.5 as percentage UNION ALL
    SELECT 'US5949181045', 4.2 UNION ALL
    SELECT 'US0231351067', 2.8 UNION ALL
    SELECT 'US02079K3059', 2.5 UNION ALL
    SELECT 'US88160R1014', 1.8 UNION ALL
    SELECT 'DE0007164600', 0.8 UNION ALL
    SELECT 'DE0007236101', 0.6 UNION ALL
    SELECT 'NL0010273215', 0.9 UNION ALL
    SELECT 'CH0038863350', 1.2 UNION ALL
    SELECT 'CH0012032048', 1.0 UNION ALL
    SELECT 'FR0000121014', 0.7 UNION ALL
    SELECT 'FR0000120271', 0.9 UNION ALL
    SELECT 'GB0005405286', 0.8 UNION ALL
    SELECT 'GB00B10RZP78', 0.6 UNION ALL
    SELECT 'JP3633400001', 1.1 UNION ALL
    SELECT 'JP3435000009', 0.5
) p
JOIN stocks s ON s.isin = p.isin
ON CONFLICT DO NOTHING;

-- Comments
COMMENT ON COLUMN stocks.country IS 'ISO 3166-1 alpha-2: US=USA, DE=Germany, NL=Netherlands, CH=Switzerland, FR=France, GB=United Kingdom, JP=Japan';
