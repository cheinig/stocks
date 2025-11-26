package com.stockstatus.dto;

import com.stockstatus.domain.ETFAllocation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Result of an import operation containing allocations and warnings
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportResult {

    /**
     * List of imported allocations
     */
    private List<ETFAllocation> allocations;

    /**
     * List of sector names that could not be mapped to GICS
     */
    private List<String> unmappedSectors;
}
