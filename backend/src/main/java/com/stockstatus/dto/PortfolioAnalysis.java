package com.stockstatus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO containing complete portfolio analysis results
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortfolioAnalysis {

    /**
     * Portfolio ID
     */
    private Long portfolioId;

    /**
     * Portfolio name
     */
    private String portfolioName;

    /**
     * All aggregated stock allocations (sorted by percentage descending)
     */
    private List<AggregatedStockAllocation> allStocks;

    /**
     * Top 20 stock allocations
     */
    private List<AggregatedStockAllocation> top20Stocks;

    /**
     * Country allocations (sorted by percentage descending)
     */
    private List<CountryAllocation> countryAllocations;

    /**
     * Total number of unique stocks in the portfolio
     */
    private Integer totalUniqueStocks;

    /**
     * Total number of countries represented
     */
    private Integer totalCountries;
}
