package com.stockstatus.domain;

/**
 * Enum representing different types of file importers for ETF allocation data
 */
public enum ImporterType {
    /**
     * Generic CSV importer - expects columns: ISIN, Name, Percentage
     */
    GENERIC_CSV("Generic CSV", "Comma-separated values with ISIN, Name, Percentage columns"),

    /**
     * Generic Excel importer - expects columns: ISIN, Name, Percentage
     */
    GENERIC_EXCEL("Generic Excel", "Excel file with ISIN, Name, Percentage columns"),

    /**
     * iShares CSV format
     */
    ISHARES_CSV("iShares CSV", "iShares specific CSV format"),

    /**
     * Vanguard CSV format
     */
    VANGUARD_CSV("Vanguard CSV", "Vanguard specific CSV format"),

    /**
     * SPDR (State Street) CSV format
     */
    SPDR_CSV("SPDR CSV", "SPDR/State Street specific CSV format"),

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
    VANECK("VanEck Excel", "VanEck ETF holdings Excel format");

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
}
