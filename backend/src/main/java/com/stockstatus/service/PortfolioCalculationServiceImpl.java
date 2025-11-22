package com.stockstatus.service;

import com.stockstatus.domain.AssetType;
import com.stockstatus.domain.ETFAllocation;
import com.stockstatus.domain.Portfolio;
import com.stockstatus.domain.PortfolioPosition;
import com.stockstatus.domain.Stock;
import com.stockstatus.dto.AggregatedStockAllocation;
import com.stockstatus.dto.CountryAllocation;
import com.stockstatus.dto.PortfolioAnalysis;
import com.stockstatus.dto.SectorAllocation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of PortfolioCalculationService
 * Performs complex calculations for portfolio analysis
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class PortfolioCalculationServiceImpl implements PortfolioCalculationService {

    private static final int PERCENTAGE_SCALE = 6;
    private static final int DISPLAY_SCALE = 4;

    private final PortfolioService portfolioService;
    private final StockService stockService;
    private final ETFService etfService;

    @Override
    public List<AggregatedStockAllocation> calculateAggregatedStockAllocations(Long portfolioId) {
        log.info("Calculating aggregated stock allocations for portfolio ID: {}", portfolioId);

        Portfolio portfolio = portfolioService.findByIdWithPositions(portfolioId);
        List<PortfolioPosition> positions = portfolio.getPositions();

        if (positions.isEmpty()) {
            log.debug("Portfolio has no positions, returning empty list");
            return new ArrayList<>();
        }

        // Calculate total portfolio value (sum of all quantities)
        BigDecimal totalValue = positions.stream()
            .map(PortfolioPosition::getQuantity)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalValue.compareTo(BigDecimal.ZERO) == 0) {
            log.warn("Total portfolio value is zero");
            return new ArrayList<>();
        }

        log.debug("Total portfolio value: {}", totalValue);

        // Map to store aggregated allocations by ISIN
        Map<String, StockAllocationAccumulator> allocationMap = new HashMap<>();

        // Process each position
        for (PortfolioPosition position : positions) {
            BigDecimal positionPercentage = position.getQuantity()
                .divide(totalValue, PERCENTAGE_SCALE, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));

            log.debug("Processing position: {} {} with {}% of portfolio",
                     position.getAssetType(), position.getAssetId(), positionPercentage);

            if (position.getAssetType() == AssetType.STOCK) {
                processStockPosition(position, positionPercentage, allocationMap);
            } else if (position.getAssetType() == AssetType.ETF) {
                processEtfPosition(position, positionPercentage, allocationMap);
            }
        }

        // Convert map to list and sort by total percentage descending
        List<AggregatedStockAllocation> result = allocationMap.values().stream()
            .map(this::convertToDto)
            .sorted(Comparator.comparing(AggregatedStockAllocation::getTotalPercentage).reversed())
            .collect(Collectors.toList());

        log.info("Calculated {} unique stock allocations for portfolio ID: {}", result.size(), portfolioId);
        return result;
    }

    @Override
    public List<CountryAllocation> calculateCountryAllocations(Long portfolioId) {
        log.info("Calculating country allocations for portfolio ID: {}", portfolioId);

        List<AggregatedStockAllocation> stockAllocations = calculateAggregatedStockAllocations(portfolioId);

        // Group by country and sum percentages
        Map<String, CountryAllocationAccumulator> countryMap = new HashMap<>();

        for (AggregatedStockAllocation stock : stockAllocations) {
            String country = stock.getCountry() != null ? stock.getCountry() : "XX";
            countryMap.computeIfAbsent(country, k -> new CountryAllocationAccumulator(k))
                .addPercentage(stock.getTotalPercentage());
        }

        List<CountryAllocation> result = countryMap.values().stream()
            .map(acc -> CountryAllocation.builder()
                .countryCode(acc.countryCode)
                .percentage(acc.totalPercentage.setScale(DISPLAY_SCALE, RoundingMode.HALF_UP))
                .stockCount(acc.stockCount)
                .build())
            .sorted(Comparator.comparing(CountryAllocation::getPercentage).reversed())
            .collect(Collectors.toList());

        log.info("Calculated allocations for {} countries", result.size());
        return result;
    }

    @Override
    public List<AggregatedStockAllocation> getTopStocks(Long portfolioId, int limit) {
        log.info("Getting top {} stocks for portfolio ID: {}", limit, portfolioId);

        List<AggregatedStockAllocation> allStocks = calculateAggregatedStockAllocations(portfolioId);

        return allStocks.stream()
            .limit(limit)
            .collect(Collectors.toList());
    }

    @Override
    public PortfolioAnalysis calculatePortfolioAnalysis(Long portfolioId) {
        log.info("Calculating complete portfolio analysis for portfolio ID: {}", portfolioId);

        Portfolio portfolio = portfolioService.findById(portfolioId);
        List<AggregatedStockAllocation> allStocks = calculateAggregatedStockAllocations(portfolioId);
        List<AggregatedStockAllocation> top20 = allStocks.stream().limit(20).collect(Collectors.toList());
        List<CountryAllocation> countryAllocations = calculateCountryAllocations(portfolioId);

        PortfolioAnalysis analysis = PortfolioAnalysis.builder()
            .portfolioId(portfolio.getId())
            .portfolioName(portfolio.getName())
            .allStocks(allStocks)
            .top20Stocks(top20)
            .countryAllocations(countryAllocations)
            .totalUniqueStocks(allStocks.size())
            .totalCountries(countryAllocations.size())
            .build();

        log.info("Portfolio analysis complete: {} unique stocks, {} countries",
                 analysis.getTotalUniqueStocks(), analysis.getTotalCountries());

        return analysis;
    }

    /**
     * Process a stock position and add it to the allocation map
     */
    private void processStockPosition(PortfolioPosition position, BigDecimal positionPercentage,
                                     Map<String, StockAllocationAccumulator> allocationMap) {
        Stock stock = stockService.findById(position.getAssetId());

        StockAllocationAccumulator accumulator = allocationMap.computeIfAbsent(
            stock.getIsin(),
            isin -> new StockAllocationAccumulator(stock)
        );

        accumulator.addDirectPercentage(positionPercentage);
        log.debug("Added direct stock position: {} ({}), percentage: {}%",
                 stock.getName(), stock.getIsin(), positionPercentage);
    }

    /**
     * Process an ETF position, expand it into underlying stocks, and add to allocation map
     */
    private void processEtfPosition(PortfolioPosition position, BigDecimal positionPercentage,
                                   Map<String, StockAllocationAccumulator> allocationMap) {
        List<ETFAllocation> etfAllocations = etfService.getCurrentAllocation(position.getAssetId());

        if (etfAllocations.isEmpty()) {
            log.warn("ETF ID {} has no allocation data, skipping", position.getAssetId());
            return;
        }

        log.debug("Expanding ETF position into {} underlying stocks", etfAllocations.size());

        for (ETFAllocation etfAllocation : etfAllocations) {
            Stock stock = etfAllocation.getStock();

            // Calculate effective percentage: ETF position % * stock allocation %
            BigDecimal effectivePercentage = positionPercentage
                .multiply(etfAllocation.getPercentage())
                .divide(new BigDecimal("100"), PERCENTAGE_SCALE, RoundingMode.HALF_UP);

            StockAllocationAccumulator accumulator = allocationMap.computeIfAbsent(
                stock.getIsin(),
                isin -> new StockAllocationAccumulator(stock)
            );

            accumulator.addEtfPercentage(effectivePercentage);
            log.trace("Added ETF stock: {} ({}), effective percentage: {}%",
                     stock.getName(), stock.getIsin(), effectivePercentage);
        }
    }

    /**
     * Convert accumulator to DTO
     */
    private AggregatedStockAllocation convertToDto(StockAllocationAccumulator accumulator) {
        return AggregatedStockAllocation.builder()
            .isin(accumulator.stock.getIsin())
            .name(accumulator.stock.getName())
            .country(accumulator.stock.getCountry())
            .sector(accumulator.stock.getSector())
            .totalPercentage(accumulator.getTotalPercentage().setScale(DISPLAY_SCALE, RoundingMode.HALF_UP))
            .directPercentage(accumulator.directPercentage.setScale(DISPLAY_SCALE, RoundingMode.HALF_UP))
            .etfPercentage(accumulator.etfPercentage.setScale(DISPLAY_SCALE, RoundingMode.HALF_UP))
            .etfCount(accumulator.etfSources.size())
            .build();
    }

    /**
     * Helper class to accumulate stock allocations from multiple sources
     */
    private static class StockAllocationAccumulator {
        private final Stock stock;
        private BigDecimal directPercentage = BigDecimal.ZERO;
        private BigDecimal etfPercentage = BigDecimal.ZERO;
        private final Set<Long> etfSources = new HashSet<>();

        public StockAllocationAccumulator(Stock stock) {
            this.stock = stock;
        }

        public void addDirectPercentage(BigDecimal percentage) {
            this.directPercentage = this.directPercentage.add(percentage);
        }

        public void addEtfPercentage(BigDecimal percentage) {
            this.etfPercentage = this.etfPercentage.add(percentage);
        }

        public BigDecimal getTotalPercentage() {
            return directPercentage.add(etfPercentage);
        }
    }

    @Override
    public List<SectorAllocation> calculateSectorAllocations(Long portfolioId) {
        log.info("Calculating sector allocations for portfolio ID: {}", portfolioId);

        List<AggregatedStockAllocation> stockAllocations = calculateAggregatedStockAllocations(portfolioId);

        Map<String, SectorAllocationAccumulator> sectorMap = new HashMap<>();

        for (AggregatedStockAllocation allocation : stockAllocations) {
            String sector = allocation.getSector();
            SectorAllocationAccumulator accumulator = sectorMap.computeIfAbsent(
                sector,
                s -> new SectorAllocationAccumulator(s)
            );
            accumulator.addPercentage(allocation.getTotalPercentage());
        }

        List<SectorAllocation> result = sectorMap.values().stream()
            .map(accumulator -> SectorAllocation.builder()
                .sector(accumulator.sector)
                .percentage(accumulator.totalPercentage.setScale(DISPLAY_SCALE, RoundingMode.HALF_UP).doubleValue())
                .stockCount(accumulator.stockCount)
                .build())
            .sorted(Comparator.comparing(SectorAllocation::getPercentage).reversed())
            .collect(Collectors.toList());

        log.info("Calculated {} sector allocations for portfolio {}", result.size(), portfolioId);
        return result;
    }

    /**
     * Helper class to accumulate country allocations
     */
    private static class CountryAllocationAccumulator {
        private final String countryCode;
        private BigDecimal totalPercentage = BigDecimal.ZERO;
        private int stockCount = 0;

        public CountryAllocationAccumulator(String countryCode) {
            this.countryCode = countryCode;
        }

        public void addPercentage(BigDecimal percentage) {
            this.totalPercentage = this.totalPercentage.add(percentage);
            this.stockCount++;
        }
    }

    /**
     * Helper class to accumulate sector allocations
     */
    private static class SectorAllocationAccumulator {
        private final String sector;
        private BigDecimal totalPercentage = BigDecimal.ZERO;
        private int stockCount = 0;

        public SectorAllocationAccumulator(String sector) {
            this.sector = sector;
        }

        public void addPercentage(BigDecimal percentage) {
            this.totalPercentage = this.totalPercentage.add(percentage);
            this.stockCount++;
        }
    }
}
