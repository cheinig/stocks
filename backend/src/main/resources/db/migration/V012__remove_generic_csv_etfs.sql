-- Remove ETFs with obsolete GENERIC importer types
-- GENERIC_CSV and GENERIC_EXCEL importers were removed from the codebase and are no longer supported
-- Valid importer types are: FIDELITY, XTRACKERS, VANECK, AMUNDI, ISHARES_WEB, XTRACKERS_WEB, VANECK_WEB

-- First, delete all allocations for ETFs with obsolete importer types
-- This is necessary due to foreign key constraints
DELETE FROM etf_allocations
WHERE etf_id IN (
    SELECT id FROM etfs WHERE importer_type IN ('GENERIC_CSV', 'GENERIC_EXCEL')
);

-- Delete all portfolio positions for ETFs with obsolete importer types
-- This is necessary due to foreign key constraints
-- portfolio_positions uses asset_type='ETF' and asset_id to reference ETFs
DELETE FROM portfolio_positions
WHERE asset_type = 'ETF'
  AND asset_id IN (
    SELECT id FROM etfs WHERE importer_type IN ('GENERIC_CSV', 'GENERIC_EXCEL')
);

-- Now delete the ETFs themselves
DELETE FROM etfs WHERE importer_type IN ('GENERIC_CSV', 'GENERIC_EXCEL');

-- Log the cleanup
COMMENT ON COLUMN etfs.importer_type IS 'Type of importer to use for allocation data. Valid types: FIDELITY, XTRACKERS, VANECK, AMUNDI, ISHARES_WEB, XTRACKERS_WEB, VANECK_WEB. GENERIC_CSV and GENERIC_EXCEL were removed.';
