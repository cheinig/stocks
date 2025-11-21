package com.stockstatus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO representing country allocation in a portfolio
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CountryAllocation {

    /**
     * The country code (ISO 3166-1 alpha-2)
     */
    private String countryCode;

    /**
     * Total percentage of portfolio allocated to this country
     */
    private BigDecimal percentage;

    /**
     * Number of stocks from this country in the portfolio
     */
    private Integer stockCount;
}
