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
     * Amundi Excel format
     */
    AMUNDI("Amundi Excel", "Amundi ETF holdings Excel format"),

    /**
     * iShares Web importer - fetches holdings from iShares website
     */
    ISHARES_WEB("iShares Web", "iShares ETF holdings from website"),

    /**
     * XTrackers Web importer - fetches holdings from XTrackers (DWS) website
     */
    XTRACKERS_WEB("XTrackers Web", "XTrackers ETF holdings from website"),

    /**
     * VanEck Web importer - fetches holdings from VanEck website
     */
    VANECK_WEB("VanEck Web", "VanEck ETF holdings from website"),

    /**
     * Amundi Web importer - fetches holdings from Amundi website
     */
    AMUNDI_WEB("Amundi Web", "Amundi ETF holdings from website"),

    /**
     * Fidelity Web importer - fetches holdings from Fidelity website
     */
    FIDELITY_WEB("Fidelity Web", "Fidelity ETF holdings from website");

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
        return this == ISHARES_WEB || this == XTRACKERS_WEB || this == VANECK_WEB || this == AMUNDI_WEB || this == FIDELITY_WEB;
    }

    /**
     * Check if this web importer requires a webDataId parameter
     * @return true if this web importer needs webDataId
     */
    public boolean requiresWebDataId() {
        return this == ISHARES_WEB;
    }

    /**
     * Check if this web importer requires a ticker symbol parameter
     * @return true if this web importer needs ticker symbol
     */
    public boolean requiresTickerSymbol() {
        return this == VANECK_WEB;
    }
}
