package com.stockstatus.web;

import com.stockstatus.dto.AggregatedStockAllocation;
import com.stockstatus.dto.CountryAllocation;
import com.stockstatus.dto.PortfolioAnalysis;
import com.stockstatus.dto.SectorAllocation;
import com.stockstatus.service.PortfolioCalculationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Dashboard and Analytics
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final PortfolioCalculationService calculationService;

    /**
     * Get complete portfolio analysis
     * GET /api/dashboard/analysis/{portfolioId}
     */
    @GetMapping("/analysis/{portfolioId}")
    public ResponseEntity<PortfolioAnalysis> getPortfolioAnalysis(@PathVariable Long portfolioId) {
        log.info("REST request to get portfolio analysis for Portfolio ID: {}", portfolioId);

        PortfolioAnalysis analysis = calculationService.calculatePortfolioAnalysis(portfolioId);

        return ResponseEntity.ok(analysis);
    }

    /**
     * Get country allocation for a portfolio
     * GET /api/dashboard/country-allocation/{portfolioId}
     */
    @GetMapping("/country-allocation/{portfolioId}")
    public ResponseEntity<List<CountryAllocation>> getCountryAllocation(@PathVariable Long portfolioId) {
        log.info("REST request to get country allocation for Portfolio ID: {}", portfolioId);

        List<CountryAllocation> countryAllocations = calculationService.calculateCountryAllocations(portfolioId);

        return ResponseEntity.ok(countryAllocations);
    }

    /**
     * Get top N stocks for a portfolio
     * GET /api/dashboard/top-stocks/{portfolioId}?limit=20
     */
    @GetMapping("/top-stocks/{portfolioId}")
    public ResponseEntity<List<AggregatedStockAllocation>> getTopStocks(
        @PathVariable Long portfolioId,
        @RequestParam(defaultValue = "20") int limit
    ) {
        log.info("REST request to get top {} stocks for Portfolio ID: {}", limit, portfolioId);

        List<AggregatedStockAllocation> topStocks = calculationService.getTopStocks(portfolioId, limit);

        return ResponseEntity.ok(topStocks);
    }

    /**
     * Get aggregated stock allocations for a portfolio
     * GET /api/dashboard/stock-allocations/{portfolioId}
     */
    @GetMapping("/stock-allocations/{portfolioId}")
    public ResponseEntity<List<AggregatedStockAllocation>> getStockAllocations(@PathVariable Long portfolioId) {
        log.info("REST request to get stock allocations for Portfolio ID: {}", portfolioId);

        List<AggregatedStockAllocation> allocations = calculationService.calculateAggregatedStockAllocations(portfolioId);

        return ResponseEntity.ok(allocations);
    }

    /**
     * Get sector allocation for a portfolio
     * GET /api/dashboard/sector-allocation/{portfolioId}
     */
    @GetMapping("/sector-allocation/{portfolioId}")
    public ResponseEntity<List<SectorAllocation>> getSectorAllocation(@PathVariable Long portfolioId) {
        log.info("REST request to get sector allocation for Portfolio ID: {}", portfolioId);

        List<SectorAllocation> sectorAllocations = calculationService.calculateSectorAllocations(portfolioId);

        return ResponseEntity.ok(sectorAllocations);
    }
}
