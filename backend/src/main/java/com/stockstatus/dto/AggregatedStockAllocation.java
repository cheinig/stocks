package com.stockstatus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO representing an aggregated stock allocation in a portfolio
 * Combines direct stock holdings and indirect holdings through ETFs
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AggregatedStockAllocation {

    /**
     * The ISIN of the stock
     */
    private String isin;

    /**
     * The name of the stock
     */
    private String name;

    /**
     * The country code of the stock
     */
    private String country;

    /**
     * The sector of the stock
     */
    private String sector;

    /**
     * Total percentage of portfolio allocated to this stock
     * Includes both direct holdings and indirect holdings through ETFs
     */
    private BigDecimal totalPercentage;

    /**
     * Percentage from direct stock positions
     */
    private BigDecimal directPercentage;

    /**
     * Percentage from ETF positions (aggregated)
     */
    private BigDecimal etfPercentage;

    /**
     * Number of ETFs that contain this stock
     */
    private Integer etfCount;
}
