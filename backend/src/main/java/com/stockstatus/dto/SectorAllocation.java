package com.stockstatus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for sector allocation in portfolio
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SectorAllocation {

    private String sector;
    private Double percentage;
    private Integer stockCount;
}
