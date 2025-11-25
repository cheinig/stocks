package com.stockstatus.service.importer;

import com.stockstatus.dto.AllocationEntry;

import java.util.List;

/**
 * Interface for web-based importers that fetch ETF allocation data from web sources
 */
public interface WebImporter {

    /**
     * Fetch and parse allocation data from a web URL
     * @param webUrl the URL to fetch data from
     * @return List of allocation entries parsed from the web source
     * @throws com.stockstatus.exception.InvalidFileFormatException if the web data format is invalid
     * @throws com.stockstatus.exception.AllocationSumException if allocation percentages don't sum to ~100%
     */
    List<AllocationEntry> fetchAndParse(String webUrl);

    /**
     * Get the name of this importer
     * @return importer name
     */
    String getImporterName();
}
