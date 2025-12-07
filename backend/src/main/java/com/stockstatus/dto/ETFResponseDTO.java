package com.stockstatus.dto;

import com.stockstatus.domain.ETF;
import com.stockstatus.domain.ImporterType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for ETF response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ETFResponseDTO {

    private Long id;
    private String name;
    private String isin;
    private ImporterType importerType;
    private String webUrl;
    private String webDataId;
    private String tickerSymbol;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Convert ETF entity to DTO
     */
    public static ETFResponseDTO fromEntity(ETF etf) {
        return ETFResponseDTO.builder()
            .id(etf.getId())
            .name(etf.getName())
            .isin(etf.getIsin())
            .importerType(etf.getImporterType())
            .webUrl(etf.getWebUrl())
            .webDataId(etf.getWebDataId())
            .tickerSymbol(etf.getTickerSymbol())
            .createdAt(etf.getCreatedAt())
            .updatedAt(etf.getUpdatedAt())
            .build();
    }

    /**
     * Convert DTO to ETF entity
     */
    public ETF toEntity() {
        return ETF.builder()
            .id(this.id)
            .name(this.name)
            .isin(this.isin)
            .importerType(this.importerType)
            .webUrl(this.webUrl)
            .webDataId(this.webDataId)
            .tickerSymbol(this.tickerSymbol)
            .build();
    }
}
