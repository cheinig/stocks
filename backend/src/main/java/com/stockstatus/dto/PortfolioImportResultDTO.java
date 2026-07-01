package com.stockstatus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO for the result of a portfolio value import (CSV upload).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortfolioImportResultDTO {

    /**
     * Portfolio ID that was updated
     */
    private Long portfolioId;

    /**
     * Total number of value rows found in the CSV
     */
    private Integer totalRows;

    /**
     * Number of positions whose value was updated
     */
    private Integer updatedCount;

    /**
     * ISINs from the CSV that could not be matched to an existing position
     */
    @Builder.Default
    private List<String> unmatchedIsins = new ArrayList<>();

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
