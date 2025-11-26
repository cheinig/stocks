package com.stockstatus.service;

import com.stockstatus.domain.*;
import com.stockstatus.dto.AggregatedStockAllocation;
import com.stockstatus.dto.CountryAllocation;
import com.stockstatus.dto.PortfolioAnalysis;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PortfolioCalculationService
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PortfolioCalculationService Unit Tests")
class PortfolioCalculationServiceTest {

    @Mock
    private PortfolioService portfolioService;

    @Mock
    private ETFService etfService;

    @Mock
    private StockService stockService;

    @InjectMocks
    private PortfolioCalculationServiceImpl calculationService;

    private Portfolio testPortfolio;
    private Stock appleStock;
    private Stock microsoftStock;
    private ETF testETF;
    private PortfolioPosition stockPosition;
    private PortfolioPosition etfPosition;
    private ETFAllocation etfAllocation1;
    private ETFAllocation etfAllocation2;

    @BeforeEach
    void setUp() {
        appleStock = Stock.builder()
            .id(1L)
            .name("Apple Inc.")
            .isin("US0378331005")
            .country("US")
            .sector("Technology")
            .build();

        microsoftStock = Stock.builder()
            .id(2L)
            .name("Microsoft Corp.")
            .isin("US5949181045")
            .country("US")
            .sector("Technology")
            .build();

        testETF = ETF.builder()
            .id(1L)
            .name("Vanguard S&P 500")
            .isin("IE00B3XXRP09")
            .importerType(ImporterType.FIDELITY)
            .build();

        testPortfolio = Portfolio.builder()
            .id(1L)
            .name("Test Portfolio")
            .userId("user1")
            .build();

        stockPosition = PortfolioPosition.builder()
            .id(1L)
            .portfolio(testPortfolio)
            .assetType(AssetType.STOCK)
            .assetId(1L)
            .quantity(new BigDecimal("10"))
            .build();

        etfPosition = PortfolioPosition.builder()
            .id(2L)
            .portfolio(testPortfolio)
            .assetType(AssetType.ETF)
            .assetId(1L)
            .quantity(new BigDecimal("5"))
            .build();

        testPortfolio.setPositions(Arrays.asList(stockPosition, etfPosition));

        etfAllocation1 = ETFAllocation.builder()
            .id(1L)
            .etf(testETF)
            .stock(appleStock)
            .percentage(new BigDecimal("60.00"))
            .allocationVersion(1)
            .build();

        etfAllocation2 = ETFAllocation.builder()
            .id(2L)
            .etf(testETF)
            .stock(microsoftStock)
            .percentage(new BigDecimal("40.00"))
            .allocationVersion(1)
            .build();
    }

    @Test
    @DisplayName("Should calculate aggregated stock allocations with ETF expansion")
    void calculateAggregatedStockAllocations_Success() {
        // Given
        when(portfolioService.findByIdWithPositions(1L)).thenReturn(testPortfolio);
        when(etfService.getCurrentAllocation(1L)).thenReturn(Arrays.asList(etfAllocation1, etfAllocation2));
        when(stockService.findById(1L)).thenReturn(appleStock);

        // When
        List<AggregatedStockAllocation> result = calculationService.calculateAggregatedStockAllocations(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.size()).isGreaterThan(0);
        verify(portfolioService).findByIdWithPositions(1L);
        verify(etfService).getCurrentAllocation(1L);
    }

    @Test
    @DisplayName("Should calculate country allocations")
    void calculateCountryAllocations_Success() {
        // Given
        when(portfolioService.findByIdWithPositions(1L)).thenReturn(testPortfolio);
        when(etfService.getCurrentAllocation(1L)).thenReturn(Arrays.asList(etfAllocation1, etfAllocation2));
        when(stockService.findById(1L)).thenReturn(appleStock);

        // When
        List<CountryAllocation> result = calculationService.calculateCountryAllocations(1L);

        // Then
        assertThat(result).isNotNull();
        verify(portfolioService).findByIdWithPositions(1L);
    }

    @Test
    @DisplayName("Should get top N stocks")
    void getTopStocks_Success() {
        // Given
        when(portfolioService.findByIdWithPositions(1L)).thenReturn(testPortfolio);
        when(etfService.getCurrentAllocation(1L)).thenReturn(Arrays.asList(etfAllocation1, etfAllocation2));
        when(stockService.findById(1L)).thenReturn(appleStock);

        // When
        List<AggregatedStockAllocation> result = calculationService.getTopStocks(1L, 5);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.size()).isLessThanOrEqualTo(5);
        verify(portfolioService).findByIdWithPositions(1L);
    }

    @Test
    @DisplayName("Should calculate complete portfolio analysis")
    void calculatePortfolioAnalysis_Success() {
        // Given
        when(portfolioService.findById(1L)).thenReturn(testPortfolio);
        when(portfolioService.findByIdWithPositions(1L)).thenReturn(testPortfolio);
        when(etfService.getCurrentAllocation(1L)).thenReturn(Arrays.asList(etfAllocation1, etfAllocation2));
        when(stockService.findById(1L)).thenReturn(appleStock);

        // When
        PortfolioAnalysis result = calculationService.calculatePortfolioAnalysis(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAllStocks()).isNotNull();
        assertThat(result.getTop20Stocks()).isNotNull();
        assertThat(result.getCountryAllocations()).isNotNull();
        verify(portfolioService).findById(1L);
    }

    @Test
    @DisplayName("Should handle portfolio with only stock positions")
    void calculateAggregatedStockAllocations_OnlyStocks() {
        // Given
        Portfolio portfolioWithOnlyStocks = Portfolio.builder()
            .id(2L)
            .name("Stock Only Portfolio")
            .userId("user1")
            .positions(Arrays.asList(stockPosition))
            .build();

        when(portfolioService.findByIdWithPositions(2L)).thenReturn(portfolioWithOnlyStocks);
        when(stockService.findById(1L)).thenReturn(appleStock);

        // When
        List<AggregatedStockAllocation> result = calculationService.calculateAggregatedStockAllocations(2L);

        // Then
        assertThat(result).isNotNull();
        verify(portfolioService).findByIdWithPositions(2L);
        verify(etfService, never()).getCurrentAllocation(any());
    }
}
