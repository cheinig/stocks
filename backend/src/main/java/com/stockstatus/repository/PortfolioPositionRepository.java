package com.stockstatus.repository;

import com.stockstatus.domain.AssetType;
import com.stockstatus.domain.Portfolio;
import com.stockstatus.domain.PortfolioPosition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for PortfolioPosition entity operations
 */
@Repository
public interface PortfolioPositionRepository extends JpaRepository<PortfolioPosition, Long> {

    /**
     * Find all positions for a specific portfolio
     * @param portfolio the portfolio to find positions for
     * @return List of positions in that portfolio
     */
    List<PortfolioPosition> findByPortfolio(Portfolio portfolio);

    /**
     * Find all positions for a specific portfolio by ID
     * @param portfolioId the portfolio ID
     * @return List of positions in that portfolio
     */
    List<PortfolioPosition> findByPortfolioId(Long portfolioId);

    /**
     * Find a specific position by portfolio, asset type, and asset ID
     * @param portfolioId the portfolio ID
     * @param assetType the asset type (STOCK or ETF)
     * @param assetId the asset ID
     * @return Optional containing the position if found
     */
    Optional<PortfolioPosition> findByPortfolioIdAndAssetTypeAndAssetId(
        Long portfolioId, AssetType assetType, Long assetId);

    /**
     * Find all positions of a specific asset type in a portfolio
     * @param portfolioId the portfolio ID
     * @param assetType the asset type (STOCK or ETF)
     * @return List of positions of that asset type
     */
    List<PortfolioPosition> findByPortfolioIdAndAssetType(Long portfolioId, AssetType assetType);

    /**
     * Find all positions referencing a specific asset
     * @param assetType the asset type (STOCK or ETF)
     * @param assetId the asset ID
     * @return List of positions referencing that asset
     */
    List<PortfolioPosition> findByAssetTypeAndAssetId(AssetType assetType, Long assetId);

    /**
     * Find all stock positions in a portfolio
     * @param portfolioId the portfolio ID
     * @return List of stock positions
     */
    @Query("SELECT p FROM PortfolioPosition p WHERE p.portfolio.id = :portfolioId AND p.assetType = 'STOCK'")
    List<PortfolioPosition> findStockPositionsByPortfolioId(@Param("portfolioId") Long portfolioId);

    /**
     * Find all ETF positions in a portfolio
     * @param portfolioId the portfolio ID
     * @return List of ETF positions
     */
    @Query("SELECT p FROM PortfolioPosition p WHERE p.portfolio.id = :portfolioId AND p.assetType = 'ETF'")
    List<PortfolioPosition> findEtfPositionsByPortfolioId(@Param("portfolioId") Long portfolioId);

    /**
     * Count positions in a portfolio
     * @param portfolioId the portfolio ID
     * @return number of positions
     */
    long countByPortfolioId(Long portfolioId);

    /**
     * Count positions of a specific asset type in a portfolio
     * @param portfolioId the portfolio ID
     * @param assetType the asset type
     * @return number of positions
     */
    long countByPortfolioIdAndAssetType(Long portfolioId, AssetType assetType);

    /**
     * Check if a position exists for a specific portfolio and asset
     * @param portfolioId the portfolio ID
     * @param assetType the asset type
     * @param assetId the asset ID
     * @return true if the position exists
     */
    boolean existsByPortfolioIdAndAssetTypeAndAssetId(Long portfolioId, AssetType assetType, Long assetId);

    /**
     * Delete all positions for a specific portfolio
     * @param portfolioId the portfolio ID
     */
    @Modifying
    @Query("DELETE FROM PortfolioPosition p WHERE p.portfolio.id = :portfolioId")
    void deleteByPortfolioId(@Param("portfolioId") Long portfolioId);

    /**
     * Delete a specific position
     * @param portfolioId the portfolio ID
     * @param assetType the asset type
     * @param assetId the asset ID
     */
    @Modifying
    @Query("DELETE FROM PortfolioPosition p WHERE p.portfolio.id = :portfolioId AND p.assetType = :assetType AND p.assetId = :assetId")
    void deleteByPortfolioIdAndAssetTypeAndAssetId(
        @Param("portfolioId") Long portfolioId,
        @Param("assetType") AssetType assetType,
        @Param("assetId") Long assetId);
}
