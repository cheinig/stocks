package com.stockstatus.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO representing a single allocation entry from an imported file
 * Contains the stock ISIN, name, and percentage allocation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AllocationEntry {

    /**
     * ISIN of the stock (optional for some importers)
     * Can be empty, standard ISIN format, or special identifier (e.g., SONSTIGE00000)
     */
    @Pattern(regexp = "^$|^([A-Z]{2}[A-Z0-9]{9}[0-9]|SONSTIGE[0-9]{5})$",
             message = "ISIN must be empty, a valid ISIN format, or special identifier")
    private String isin;

    /**
     * Name of the stock
     */
    @NotBlank(message = "Stock name is required")
    private String name;

    /**
     * Percentage allocation (0-100)
     */
    @NotNull(message = "Percentage is required")
    @DecimalMin(value = "0.000001", message = "Percentage must be greater than 0")
    @DecimalMax(value = "100.0", message = "Percentage must not exceed 100")
    private BigDecimal percentage;

    /**
     * Optional country code (ISO 3166-1 alpha-2)
     */
    @Pattern(regexp = "^[A-Z]{2}$", message = "Country must be a valid ISO 3166-1 alpha-2 code")
    private String country;

    /**
     * Optional sector
     */
    private String sector;

    /**
     * Original sector name from import file (for tracking unmapped sectors)
     * This field is set when the sector value couldn't be mapped to a GICS sector
     */
    private String originalSector;
}
