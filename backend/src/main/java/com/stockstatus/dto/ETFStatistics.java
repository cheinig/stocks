package com.stockstatus.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * DTO for ETF statistics including country and sector allocations
 */
@Data
@Builder
public class ETFStatistics {
    private Long etfId;
    private String etfName;
    private Integer totalStocks;
    private Integer totalCountries;
    private List<CountryAllocation> countryAllocations;
    private List<SectorAllocation> sectorAllocations;
}
