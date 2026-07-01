package com.stockstatus.service;

import com.stockstatus.domain.AssetType;
import com.stockstatus.domain.Portfolio;
import com.stockstatus.domain.PortfolioPosition;
import com.stockstatus.dto.PortfolioImportResultDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service for managing portfolios and positions
 */
public interface PortfolioService {

    /**
     * Create a new portfolio
     * @param portfolio the portfolio to create
     * @return the created portfolio
     * @throws com.stockstatus.exception.DuplicateResourceException if a portfolio with the same name already exists for this user
     */
    Portfolio createPortfolio(Portfolio portfolio);

    /**
     * Update an existing portfolio
     * @param id the portfolio ID
     * @param portfolio the updated portfolio data
     * @return the updated portfolio
     * @throws com.stockstatus.exception.ResourceNotFoundException if portfolio not found
     */
    Portfolio updatePortfolio(Long id, Portfolio portfolio);

    /**
     * Delete a portfolio
     * @param id the portfolio ID
     * @throws com.stockstatus.exception.ResourceNotFoundException if portfolio not found
     */
    void deletePortfolio(Long id);

    /**
     * Find a portfolio by ID
     * @param id the portfolio ID
     * @return the portfolio
     * @throws com.stockstatus.exception.ResourceNotFoundException if portfolio not found
     */
    Portfolio findById(Long id);

    /**
     * Find a portfolio by ID with all positions eagerly loaded
     * @param id the portfolio ID
     * @return the portfolio with positions
     * @throws com.stockstatus.exception.ResourceNotFoundException if portfolio not found
     */
    Portfolio findByIdWithPositions(Long id);

    /**
     * Find all portfolios
     * @param pageable pagination information
     * @return page of portfolios
     */
    Page<Portfolio> findAll(Pageable pageable);

    /**
     * Add a position to a portfolio
     * @param portfolioId the portfolio ID
     * @param assetType the type of asset (STOCK or ETF)
     * @param assetId the ID of the asset (stock or ETF)
     * @param quantity the quantity of the asset
     * @return the created position
     * @throws com.stockstatus.exception.ResourceNotFoundException if portfolio, stock, or ETF not found
     * @throws com.stockstatus.exception.DuplicateResourceException if position already exists
     */
    PortfolioPosition addPosition(Long portfolioId, AssetType assetType, Long assetId, BigDecimal quantity);

    /**
     * Update a position's quantity
     * @param positionId the position ID
     * @param quantity the new quantity
     * @return the updated position
     * @throws com.stockstatus.exception.ResourceNotFoundException if position not found
     */
    PortfolioPosition updatePosition(Long positionId, BigDecimal quantity);

    /**
     * Remove a position from a portfolio
     * @param positionId the position ID
     * @throws com.stockstatus.exception.ResourceNotFoundException if position not found
     */
    void removePosition(Long positionId);

    /**
     * Get all positions for a portfolio
     * @param portfolioId the portfolio ID
     * @return list of positions
     * @throws com.stockstatus.exception.ResourceNotFoundException if portfolio not found
     */
    List<PortfolioPosition> getPositions(Long portfolioId);

    /**
     * Get only stock positions for a portfolio
     * @param portfolioId the portfolio ID
     * @return list of stock positions
     * @throws com.stockstatus.exception.ResourceNotFoundException if portfolio not found
     */
    List<PortfolioPosition> getStockPositions(Long portfolioId);

    /**
     * Get only ETF positions for a portfolio
     * @param portfolioId the portfolio ID
     * @return list of ETF positions
     * @throws com.stockstatus.exception.ResourceNotFoundException if portfolio not found
     */
    List<PortfolioPosition> getEtfPositions(Long portfolioId);

    /**
     * Import current position values from a portfolio CSV export.
     * For each ISIN in the file the matching position's value (quantity) is
     * overwritten with the rounded euro value. ISINs without a matching position
     * are skipped and reported.
     * @param portfolioId the portfolio ID
     * @param file the CSV file
     * @return statistics about the import (updated count, unmatched ISINs, warnings)
     * @throws com.stockstatus.exception.ResourceNotFoundException if portfolio not found
     * @throws com.stockstatus.exception.InvalidFileFormatException if the file cannot be parsed
     */
    PortfolioImportResultDTO importPositionValues(Long portfolioId, MultipartFile file);
}
