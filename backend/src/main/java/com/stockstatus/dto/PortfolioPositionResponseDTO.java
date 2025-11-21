package com.stockstatus.dto;

import com.stockstatus.domain.AssetType;
import com.stockstatus.domain.PortfolioPosition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for Portfolio Position response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortfolioPositionResponseDTO {

    private Long id;
    private Long portfolioId;
    private AssetType assetType;
    private Long assetId;
    private BigDecimal quantity;
    private String assetName;
    private String assetIsin;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Convert PortfolioPosition entity to DTO
     */
    public static PortfolioPositionResponseDTO fromEntity(PortfolioPosition position) {
        return PortfolioPositionResponseDTO.builder()
            .id(position.getId())
            .portfolioId(position.getPortfolio() != null ? position.getPortfolio().getId() : null)
            .assetType(position.getAssetType())
            .assetId(position.getAssetId())
            .quantity(position.getQuantity())
            .createdAt(position.getCreatedAt())
            .updatedAt(position.getUpdatedAt())
            .build();
    }
}
