package com.stockstatus.dto;

import com.stockstatus.domain.Stock;
import com.stockstatus.util.SectorNormalizer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for Stock response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockResponseDTO {

    private Long id;
    private String name;
    private String isin;
    private String country;
    private String sector;
    private boolean hasLogo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Convert Stock entity to DTO
     */
    public static StockResponseDTO fromEntity(Stock stock) {
        return StockResponseDTO.builder()
            .id(stock.getId())
            .name(stock.getName())
            .isin(stock.getIsin())
            .country(stock.getCountry())
            .sector(SectorNormalizer.normalize(stock.getSector()))
            .hasLogo(stock.getLogo() != null && stock.getLogo().length > 0)
            .createdAt(stock.getCreatedAt())
            .updatedAt(stock.getUpdatedAt())
            .build();
    }

    /**
     * Convert DTO to Stock entity
     */
    public Stock toEntity() {
        return Stock.builder()
            .id(this.id)
            .name(this.name)
            .isin(this.isin)
            .country(this.country)
            .sector(this.sector)
            .build();
    }
}
