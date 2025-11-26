package com.stockstatus.domain;

/**
 * Enum representing different types of file importers for ETF allocation data
 */
public enum ImporterType {
    /**
     * Fidelity Excel format
     */
    FIDELITY("Fidelity Excel", "Fidelity ETF holdings Excel format"),

    /**
     * Xtrackers Excel format
     */
    XTRACKERS("Xtrackers Excel", "Xtrackers ETF holdings Excel format"),

    /**
     * VanEck Excel format
     */
    VANECK("VanEck Excel", "VanEck ETF holdings Excel format"),

    /**
     * iShares Web importer - fetches holdings from iShares website
     */
    ISHARES_WEB("iShares Web", "iShares ETF holdings from website"),

    /**
     * Amundi Excel format
     */
    AMUNDI("Amundi Excel", "Amundi ETF holdings Excel format");

    private final String displayName;
    private final String description;

    ImporterType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Check if this importer type is a web-based importer
     * @return true if this is a web importer
     */
    public boolean isWebImporter() {
        return this == ISHARES_WEB;
    }
}
