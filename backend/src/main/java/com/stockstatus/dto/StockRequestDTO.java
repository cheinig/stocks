package com.stockstatus.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating or updating a Stock
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockRequestDTO {

    @NotBlank(message = "Stock name is required")
    @Size(max = 255, message = "Stock name must not exceed 255 characters")
    private String name;

    @NotBlank(message = "ISIN is required")
    @Pattern(regexp = "^[A-Z]{2}[A-Z0-9]{9}[0-9]$", message = "Invalid ISIN format. Expected format: 2 letters + 9 alphanumeric + 1 digit")
    private String isin;

    @NotBlank(message = "Country code is required")
    @Pattern(regexp = "^[A-Z]{2}$", message = "Country code must be 2 uppercase letters (ISO 3166-1 alpha-2)")
    @Size(min = 2, max = 2, message = "Country code must be exactly 2 characters")
    private String country;

    @Size(max = 100, message = "Sector must not exceed 100 characters")
    private String sector;
}
