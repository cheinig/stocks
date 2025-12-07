package com.stockstatus.util;

import java.util.Set;

/**
 * Utility class to normalize sector names to known GICS sectors
 */
public class SectorNormalizer {

    private static final Set<String> VALID_GICS_SECTORS = Set.of(
        "Information Technology",
        "Health Care",
        "Financials",
        "Consumer Discretionary",
        "Consumer Staples",
        "Industrials",
        "Energy",
        "Materials",
        "Real Estate",
        "Communication Services",
        "Utilities",
        "Unbekannt"
    );

    private static final String UNKNOWN_SECTOR = "Unbekannt";

    /**
     * Normalize a sector name to a known GICS sector.
     * Returns "Unbekannt" if the sector is null, empty, or not in the valid GICS sectors list.
     *
     * @param sector the sector name to normalize
     * @return the normalized sector name
     */
    public static String normalize(String sector) {
        // Handle null or empty/blank sectors
        if (sector == null || sector.trim().isEmpty()) {
            return UNKNOWN_SECTOR;
        }

        // Trim whitespace
        String trimmedSector = sector.trim();

        // Return the sector if it's valid, otherwise return "Unbekannt"
        return VALID_GICS_SECTORS.contains(trimmedSector) ? trimmedSector : UNKNOWN_SECTOR;
    }

    /**
     * Check if a sector is a valid GICS sector
     *
     * @param sector the sector name to check
     * @return true if the sector is valid, false otherwise
     */
    public static boolean isValid(String sector) {
        return sector != null && VALID_GICS_SECTORS.contains(sector.trim());
    }
}
