package com.stockstatus.dto;

import com.stockstatus.domain.AssetType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for creating or updating a Portfolio Position
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortfolioPositionRequestDTO {

    @NotNull(message = "Asset type is required")
    private AssetType assetType;

    @NotNull(message = "Asset ID is required")
    private Long assetId;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.000001", message = "Quantity must be positive")
    private BigDecimal quantity;
}
