-- Migration to normalize all sector values to GICS standard
-- This updates all non-GICS sector values to "Unbekannt"

-- GICS (Global Industry Classification Standard) Sektoren:
-- - Communication Services
-- - Consumer Discretionary
-- - Consumer Staples
-- - Energy
-- - Financials
-- - Health Care
-- - Industrials
-- - Information Technology
-- - Materials
-- - Real Estate
-- - Utilities
-- - Unbekannt

-- Update all stocks with non-GICS sector values to "Unbekannt"
UPDATE stocks
SET sector = 'Unbekannt'
WHERE sector IS NOT NULL
  AND sector NOT IN (
    'Communication Services',
    'Consumer Discretionary',
    'Consumer Staples',
    'Energy',
    'Financials',
    'Health Care',
    'Industrials',
    'Information Technology',
    'Materials',
    'Real Estate',
    'Utilities',
    'Unbekannt'
  );

-- Also update NULL sectors to "Unbekannt"
UPDATE stocks
SET sector = 'Unbekannt'
WHERE sector IS NULL;
