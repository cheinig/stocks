package com.stockstatus.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating or updating a Portfolio
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortfolioRequestDTO {

    @NotBlank(message = "Portfolio name is required")
    @Size(max = 255, message = "Portfolio name must not exceed 255 characters")
    private String name;

    @Size(max = 100, message = "User ID must not exceed 100 characters")
    private String userId;
}
