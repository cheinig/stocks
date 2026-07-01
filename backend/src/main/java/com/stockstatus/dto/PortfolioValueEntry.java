package com.stockstatus.dto;

import java.math.BigDecimal;

/**
 * A single ISIN -> current value entry parsed from a portfolio CSV export.
 * The value is already rounded to full euros.
 */
public record PortfolioValueEntry(String isin, BigDecimal value) {
}
