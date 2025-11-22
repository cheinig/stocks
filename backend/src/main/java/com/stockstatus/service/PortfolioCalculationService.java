package com.stockstatus.service;

import com.stockstatus.dto.AggregatedStockAllocation;
import com.stockstatus.dto.CountryAllocation;
import com.stockstatus.dto.PortfolioAnalysis;
import com.stockstatus.dto.SectorAllocation;

import java.util.List;

/**
 * Service for calculating portfolio analytics and aggregations
 * Handles complex calculations like ETF expansion and stock aggregation
 */
public interface PortfolioCalculationService {

    /**
     * Calculate aggregated stock allocations for a portfolio
     * This expands ETF positions into their underlying stocks and aggregates with direct stock positions
     *
     * Algorithm:
     * 1. Get all positions (stocks + ETFs) from portfolio
     * 2. For direct stock positions: calculate percentage = (quantity / total_portfolio_value) * 100
     * 3. For ETF positions:
     *    - Get latest ETF allocation
     *    - For each stock in ETF: effective_percentage = (etf_position_percentage * stock_allocation_percentage)
     * 4. Aggregate all stocks by ISIN, summing up percentages
     *
     * @param portfolioId the portfolio ID
     * @return List of aggregated stock allocations, sorted by total percentage descending
     * @throws com.stockstatus.exception.ResourceNotFoundException if portfolio not found
     */
    List<AggregatedStockAllocation> calculateAggregatedStockAllocations(Long portfolioId);

    /**
     * Calculate country allocations for a portfolio
     * Aggregates all stock allocations (direct + through ETFs) by country
     *
     * @param portfolioId the portfolio ID
     * @return List of country allocations, sorted by percentage descending
     * @throws com.stockstatus.exception.ResourceNotFoundException if portfolio not found
     */
    List<CountryAllocation> calculateCountryAllocations(Long portfolioId);

    /**
     * Calculate sector allocations for a portfolio
     * Aggregates all stock allocations (direct + through ETFs) by sector
     *
     * @param portfolioId the portfolio ID
     * @return List of sector allocations, sorted by percentage descending
     * @throws com.stockstatus.exception.ResourceNotFoundException if portfolio not found
     */
    List<SectorAllocation> calculateSectorAllocations(Long portfolioId);

    /**
     * Get top N stock allocations for a portfolio
     *
     * @param portfolioId the portfolio ID
     * @param limit number of top stocks to return
     * @return List of top N stock allocations, sorted by total percentage descending
     * @throws com.stockstatus.exception.ResourceNotFoundException if portfolio not found
     */
    List<AggregatedStockAllocation> getTopStocks(Long portfolioId, int limit);

    /**
     * Calculate complete portfolio analysis
     * Includes all aggregated stocks, top 20 stocks, and country allocations
     *
     * @param portfolioId the portfolio ID
     * @return Complete portfolio analysis
     * @throws com.stockstatus.exception.ResourceNotFoundException if portfolio not found
     */
    PortfolioAnalysis calculatePortfolioAnalysis(Long portfolioId);
}
