package com.stockstatus.dto;

import com.stockstatus.domain.ETFAllocation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for ETF Allocation response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ETFAllocationResponseDTO {

    private Long id;
    private Long etfId;
    private String etfName;
    private Long stockId;
    private String stockIsin;
    private String stockName;
    private BigDecimal percentage;
    private Integer allocationVersion;
    private LocalDateTime uploadDate;

    /**
     * Convert ETFAllocation entity to DTO
     */
    public static ETFAllocationResponseDTO fromEntity(ETFAllocation allocation) {
        return ETFAllocationResponseDTO.builder()
            .id(allocation.getId())
            .etfId(allocation.getEtf().getId())
            .etfName(allocation.getEtf().getName())
            .stockId(allocation.getStock().getId())
            .stockIsin(allocation.getStock().getIsin())
            .stockName(allocation.getStock().getName())
            .percentage(allocation.getPercentage())
            .allocationVersion(allocation.getAllocationVersion())
            .uploadDate(allocation.getUploadDate())
            .build();
    }
}
