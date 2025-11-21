package com.stockstatus.web;

import com.stockstatus.domain.Portfolio;
import com.stockstatus.domain.PortfolioPosition;
import com.stockstatus.dto.PortfolioPositionRequestDTO;
import com.stockstatus.dto.PortfolioPositionResponseDTO;
import com.stockstatus.dto.PortfolioRequestDTO;
import com.stockstatus.dto.PortfolioResponseDTO;
import com.stockstatus.service.PortfolioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST Controller for Portfolio operations
 */
@RestController
@RequestMapping("/api/portfolios")
@RequiredArgsConstructor
@Slf4j
public class PortfolioController {

    private final PortfolioService portfolioService;

    /**
     * Create a new portfolio
     * POST /api/portfolios
     */
    @PostMapping
    public ResponseEntity<PortfolioResponseDTO> createPortfolio(@Valid @RequestBody PortfolioRequestDTO request) {
        log.info("REST request to create Portfolio: {}", request.getName());

        Portfolio portfolio = Portfolio.builder()
            .name(request.getName())
            .userId(request.getUserId())
            .build();

        Portfolio createdPortfolio = portfolioService.createPortfolio(portfolio);
        PortfolioResponseDTO response = PortfolioResponseDTO.fromEntity(createdPortfolio);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all portfolios with pagination
     * GET /api/portfolios?page=0&size=20&sort=name,asc
     */
    @GetMapping
    public ResponseEntity<Page<PortfolioResponseDTO>> getAllPortfolios(
        @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        log.debug("REST request to get all Portfolios, page: {}", pageable.getPageNumber());

        Page<Portfolio> portfolios = portfolioService.findAll(pageable);
        Page<PortfolioResponseDTO> response = portfolios.map(PortfolioResponseDTO::fromEntity);

        return ResponseEntity.ok(response);
    }

    /**
     * Get a portfolio by ID
     * GET /api/portfolios/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<PortfolioResponseDTO> getPortfolioById(@PathVariable Long id) {
        log.debug("REST request to get Portfolio by ID: {}", id);

        Portfolio portfolio = portfolioService.findById(id);
        PortfolioResponseDTO response = PortfolioResponseDTO.fromEntity(portfolio);

        return ResponseEntity.ok(response);
    }

    /**
     * Get a portfolio by ID with all positions
     * GET /api/portfolios/{id}/with-positions
     */
    @GetMapping("/{id}/with-positions")
    public ResponseEntity<PortfolioResponseDTO> getPortfolioWithPositions(@PathVariable Long id) {
        log.debug("REST request to get Portfolio with positions by ID: {}", id);

        Portfolio portfolio = portfolioService.findByIdWithPositions(id);
        PortfolioResponseDTO response = PortfolioResponseDTO.fromEntityWithPositions(portfolio);

        return ResponseEntity.ok(response);
    }

    /**
     * Update a portfolio
     * PUT /api/portfolios/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<PortfolioResponseDTO> updatePortfolio(
        @PathVariable Long id,
        @Valid @RequestBody PortfolioRequestDTO request
    ) {
        log.info("REST request to update Portfolio ID: {}", id);

        Portfolio portfolio = Portfolio.builder()
            .name(request.getName())
            .userId(request.getUserId())
            .build();

        Portfolio updatedPortfolio = portfolioService.updatePortfolio(id, portfolio);
        PortfolioResponseDTO response = PortfolioResponseDTO.fromEntity(updatedPortfolio);

        return ResponseEntity.ok(response);
    }

    /**
     * Delete a portfolio
     * DELETE /api/portfolios/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePortfolio(@PathVariable Long id) {
        log.info("REST request to delete Portfolio ID: {}", id);

        portfolioService.deletePortfolio(id);

        return ResponseEntity.noContent().build();
    }

    /**
     * Add a position to a portfolio
     * POST /api/portfolios/{id}/positions
     */
    @PostMapping("/{id}/positions")
    public ResponseEntity<PortfolioPositionResponseDTO> addPosition(
        @PathVariable Long id,
        @Valid @RequestBody PortfolioPositionRequestDTO request
    ) {
        log.info("REST request to add position to Portfolio ID: {}", id);

        PortfolioPosition position = portfolioService.addPosition(
            id,
            request.getAssetType(),
            request.getAssetId(),
            request.getQuantity()
        );

        PortfolioPositionResponseDTO response = PortfolioPositionResponseDTO.fromEntity(position);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Update a position's quantity
     * PUT /api/portfolios/positions/{positionId}
     */
    @PutMapping("/positions/{positionId}")
    public ResponseEntity<PortfolioPositionResponseDTO> updatePosition(
        @PathVariable Long positionId,
        @Valid @RequestBody PortfolioPositionRequestDTO request
    ) {
        log.info("REST request to update Position ID: {}", positionId);

        PortfolioPosition position = portfolioService.updatePosition(positionId, request.getQuantity());
        PortfolioPositionResponseDTO response = PortfolioPositionResponseDTO.fromEntity(position);

        return ResponseEntity.ok(response);
    }

    /**
     * Delete a position
     * DELETE /api/portfolios/positions/{positionId}
     */
    @DeleteMapping("/positions/{positionId}")
    public ResponseEntity<Void> deletePosition(@PathVariable Long positionId) {
        log.info("REST request to delete Position ID: {}", positionId);

        portfolioService.removePosition(positionId);

        return ResponseEntity.noContent().build();
    }

    /**
     * Get all positions for a portfolio
     * GET /api/portfolios/{id}/positions
     */
    @GetMapping("/{id}/positions")
    public ResponseEntity<List<PortfolioPositionResponseDTO>> getPositions(@PathVariable Long id) {
        log.debug("REST request to get positions for Portfolio ID: {}", id);

        List<PortfolioPosition> positions = portfolioService.getPositions(id);
        List<PortfolioPositionResponseDTO> response = positions.stream()
            .map(PortfolioPositionResponseDTO::fromEntity)
            .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Get stock positions for a portfolio
     * GET /api/portfolios/{id}/positions/stocks
     */
    @GetMapping("/{id}/positions/stocks")
    public ResponseEntity<List<PortfolioPositionResponseDTO>> getStockPositions(@PathVariable Long id) {
        log.debug("REST request to get stock positions for Portfolio ID: {}", id);

        List<PortfolioPosition> positions = portfolioService.getStockPositions(id);
        List<PortfolioPositionResponseDTO> response = positions.stream()
            .map(PortfolioPositionResponseDTO::fromEntity)
            .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Get ETF positions for a portfolio
     * GET /api/portfolios/{id}/positions/etfs
     */
    @GetMapping("/{id}/positions/etfs")
    public ResponseEntity<List<PortfolioPositionResponseDTO>> getEtfPositions(@PathVariable Long id) {
        log.debug("REST request to get ETF positions for Portfolio ID: {}", id);

        List<PortfolioPosition> positions = portfolioService.getEtfPositions(id);
        List<PortfolioPositionResponseDTO> response = positions.stream()
            .map(PortfolioPositionResponseDTO::fromEntity)
            .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }
}
