package com.stockstatus.dto;

import com.stockstatus.domain.Portfolio;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO for Portfolio response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortfolioResponseDTO {

    private Long id;
    private String name;
    private String userId;
    private Integer positionCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<PortfolioPositionResponseDTO> positions;

    /**
     * Convert Portfolio entity to DTO (without positions)
     */
    public static PortfolioResponseDTO fromEntity(Portfolio portfolio) {
        return PortfolioResponseDTO.builder()
            .id(portfolio.getId())
            .name(portfolio.getName())
            .userId(portfolio.getUserId())
            .positionCount(portfolio.getPositions() != null ? portfolio.getPositions().size() : 0)
            .createdAt(portfolio.getCreatedAt())
            .updatedAt(portfolio.getUpdatedAt())
            .build();
    }

    /**
     * Convert Portfolio entity to DTO with positions
     */
    public static PortfolioResponseDTO fromEntityWithPositions(Portfolio portfolio) {
        PortfolioResponseDTO dto = fromEntity(portfolio);
        if (portfolio.getPositions() != null) {
            dto.setPositions(
                portfolio.getPositions().stream()
                    .map(PortfolioPositionResponseDTO::fromEntity)
                    .collect(Collectors.toList())
            );
        }
        return dto;
    }
}
