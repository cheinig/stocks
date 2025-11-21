package com.stockstatus.service;

import com.stockstatus.domain.AssetType;
import com.stockstatus.domain.Portfolio;
import com.stockstatus.domain.PortfolioPosition;
import com.stockstatus.exception.DuplicateResourceException;
import com.stockstatus.exception.ResourceNotFoundException;
import com.stockstatus.repository.PortfolioPositionRepository;
import com.stockstatus.repository.PortfolioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of PortfolioService
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class PortfolioServiceImpl implements PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final PortfolioPositionRepository positionRepository;
    private final StockService stockService;
    private final ETFService etfService;

    @Override
    public Portfolio createPortfolio(Portfolio portfolio) {
        log.info("Creating new portfolio: {}", portfolio.getName());

        // Note: Duplicate checking would require user context
        // For now, we allow duplicate names across different users
        // In the future, this should check existsByUserIdAndName

        Portfolio savedPortfolio = portfolioRepository.save(portfolio);
        log.info("Created portfolio with ID: {}", savedPortfolio.getId());

        return savedPortfolio;
    }

    @Override
    public Portfolio updatePortfolio(Long id, Portfolio portfolio) {
        log.info("Updating portfolio ID: {}", id);

        Portfolio existingPortfolio = findById(id);

        // Update fields
        existingPortfolio.setName(portfolio.getName());
        existingPortfolio.setUserId(portfolio.getUserId());

        Portfolio updatedPortfolio = portfolioRepository.save(existingPortfolio);
        log.info("Updated portfolio ID: {}", id);

        return updatedPortfolio;
    }

    @Override
    public void deletePortfolio(Long id) {
        log.info("Deleting portfolio ID: {}", id);

        Portfolio portfolio = findById(id);
        portfolioRepository.delete(portfolio);

        log.info("Deleted portfolio ID: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public Portfolio findById(Long id) {
        return portfolioRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Portfolio", "id", id));
    }

    @Override
    @Transactional(readOnly = true)
    public Portfolio findByIdWithPositions(Long id) {
        return portfolioRepository.findByIdWithPositions(id)
            .orElseThrow(() -> new ResourceNotFoundException("Portfolio", "id", id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Portfolio> findAll(Pageable pageable) {
        log.debug("Finding all portfolios, page: {}", pageable.getPageNumber());
        return portfolioRepository.findAll(pageable);
    }

    @Override
    public PortfolioPosition addPosition(Long portfolioId, AssetType assetType, Long assetId, BigDecimal quantity) {
        log.info("Adding position to portfolio {}: {} {} with quantity {}",
                 portfolioId, assetType, assetId, quantity);

        // Verify portfolio exists
        Portfolio portfolio = findById(portfolioId);

        // Verify asset exists
        if (assetType == AssetType.STOCK) {
            stockService.findById(assetId);
        } else if (assetType == AssetType.ETF) {
            etfService.findById(assetId);
        }

        // Check for duplicate position
        Optional<PortfolioPosition> existingPosition = positionRepository
            .findByPortfolioIdAndAssetTypeAndAssetId(portfolioId, assetType, assetId);

        if (existingPosition.isPresent()) {
            throw new DuplicateResourceException(
                "PortfolioPosition",
                "combination of portfolio, assetType, and assetId",
                String.format("Portfolio=%d, AssetType=%s, AssetId=%d", portfolioId, assetType, assetId)
            );
        }

        // Validate quantity
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        // Create position
        PortfolioPosition position = PortfolioPosition.builder()
            .portfolio(portfolio)
            .assetType(assetType)
            .assetId(assetId)
            .quantity(quantity)
            .build();

        PortfolioPosition savedPosition = positionRepository.save(position);
        log.info("Added position with ID: {}", savedPosition.getId());

        return savedPosition;
    }

    @Override
    public PortfolioPosition updatePosition(Long positionId, BigDecimal quantity) {
        log.info("Updating position ID {} with quantity {}", positionId, quantity);

        PortfolioPosition position = positionRepository.findById(positionId)
            .orElseThrow(() -> new ResourceNotFoundException("PortfolioPosition", "id", positionId));

        // Validate quantity
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        position.setQuantity(quantity);
        PortfolioPosition updatedPosition = positionRepository.save(position);

        log.info("Updated position ID: {}", positionId);
        return updatedPosition;
    }

    @Override
    public void removePosition(Long positionId) {
        log.info("Removing position ID: {}", positionId);

        PortfolioPosition position = positionRepository.findById(positionId)
            .orElseThrow(() -> new ResourceNotFoundException("PortfolioPosition", "id", positionId));

        positionRepository.delete(position);
        log.info("Removed position ID: {}", positionId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PortfolioPosition> getPositions(Long portfolioId) {
        log.debug("Getting all positions for portfolio ID: {}", portfolioId);

        // Verify portfolio exists
        findById(portfolioId);

        return positionRepository.findByPortfolioId(portfolioId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PortfolioPosition> getStockPositions(Long portfolioId) {
        log.debug("Getting stock positions for portfolio ID: {}", portfolioId);

        // Verify portfolio exists
        findById(portfolioId);

        return positionRepository.findStockPositionsByPortfolioId(portfolioId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PortfolioPosition> getEtfPositions(Long portfolioId) {
        log.debug("Getting ETF positions for portfolio ID: {}", portfolioId);

        // Verify portfolio exists
        findById(portfolioId);

        return positionRepository.findEtfPositionsByPortfolioId(portfolioId);
    }
}
