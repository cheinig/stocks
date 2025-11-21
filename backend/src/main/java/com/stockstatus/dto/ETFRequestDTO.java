package com.stockstatus.dto;

import com.stockstatus.domain.ImporterType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating or updating an ETF
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ETFRequestDTO {

    @NotBlank(message = "ETF name is required")
    @Size(max = 255, message = "ETF name must not exceed 255 characters")
    private String name;

    @NotBlank(message = "ISIN is required")
    @Pattern(regexp = "^[A-Z]{2}[A-Z0-9]{9}[0-9]$", message = "Invalid ISIN format. Expected format: 2 letters + 9 alphanumeric + 1 digit")
    private String isin;

    @NotNull(message = "Importer type is required")
    private ImporterType importerType;
}
