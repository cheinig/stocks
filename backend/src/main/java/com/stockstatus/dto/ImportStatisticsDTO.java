package com.stockstatus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO for file import statistics
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportStatisticsDTO {

    /**
     * ETF ID that was updated
     */
    private Long etfId;

    /**
     * ETF name
     */
    private String etfName;

    /**
     * Allocation version that was created
     */
    private Integer allocationVersion;

    /**
     * Total number of allocation entries imported
     */
    private Integer totalEntries;

    /**
     * Number of new stocks that were created during import
     */
    private Integer newStocksCreated;

    /**
     * List of newly created stock ISINs
     */
    @Builder.Default
    private List<String> newStockIsins = new ArrayList<>();

    /**
     * Number of existing stocks that were found
     */
    private Integer existingStocks;

    /**
     * Any warnings that occurred during import
     */
    @Builder.Default
    private List<String> warnings = new ArrayList<>();

    /**
     * Import success status
     */
    private boolean success;

    /**
     * Error message if import failed
     */
    private String errorMessage;
}
