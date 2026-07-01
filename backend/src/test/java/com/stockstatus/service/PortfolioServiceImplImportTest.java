package com.stockstatus.service;

import com.stockstatus.domain.AssetType;
import com.stockstatus.domain.ETF;
import com.stockstatus.domain.Portfolio;
import com.stockstatus.domain.PortfolioPosition;
import com.stockstatus.domain.Stock;
import com.stockstatus.dto.PortfolioImportResultDTO;
import com.stockstatus.dto.PortfolioValueEntry;
import com.stockstatus.repository.PortfolioPositionRepository;
import com.stockstatus.repository.PortfolioRepository;
import com.stockstatus.service.importer.ZeroCsvImporter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PortfolioServiceImpl importPositionValues Tests")
class PortfolioServiceImplImportTest {

    @Mock private PortfolioRepository portfolioRepository;
    @Mock private PortfolioPositionRepository positionRepository;
    @Mock private StockService stockService;
    @Mock private ETFService etfService;
    @Mock private ZeroCsvImporter zeroCsvImporter;

    @InjectMocks private PortfolioServiceImpl portfolioService;

    private final MultipartFile file =
        new MockMultipartFile("file", "p.csv", "text/csv", "x".getBytes());

    @Test
    @DisplayName("updates matching position value and reports unmatched ISIN")
    void updatesMatchingAndReportsUnmatched() {
        Long portfolioId = 1L;
        when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(new Portfolio()));
        when(zeroCsvImporter.parseFile(file)).thenReturn(List.of(
            new PortfolioValueEntry("DE0005557508", new BigDecimal("485")), // matched stock
            new PortfolioValueEntry("XX0000000000", new BigDecimal("100"))  // no asset
        ));

        Stock stock = mock(Stock.class);
        when(stock.getId()).thenReturn(10L);
        when(stockService.findByIsin("DE0005557508")).thenReturn(Optional.of(stock));
        when(stockService.findByIsin("XX0000000000")).thenReturn(Optional.empty());
        when(etfService.findByIsin("XX0000000000")).thenReturn(Optional.empty());

        PortfolioPosition position = new PortfolioPosition();
        when(positionRepository.findByPortfolioIdAndAssetTypeAndAssetId(portfolioId, AssetType.STOCK, 10L))
            .thenReturn(Optional.of(position));

        PortfolioImportResultDTO result = portfolioService.importPositionValues(portfolioId, file);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getUpdatedCount()).isEqualTo(1);
        assertThat(result.getUnmatchedIsins()).containsExactly("XX0000000000");
        assertThat(position.getQuantity()).isEqualByComparingTo("485");
        verify(positionRepository).save(position);
    }

    @Test
    @DisplayName("matches ETF positions by ISIN")
    void matchesEtfPositions() {
        Long portfolioId = 1L;
        when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(new Portfolio()));
        when(zeroCsvImporter.parseFile(file)).thenReturn(List.of(
            new PortfolioValueEntry("IE0009HF1MK9", new BigDecimal("614"))
        ));

        when(stockService.findByIsin("IE0009HF1MK9")).thenReturn(Optional.empty());
        ETF etf = mock(ETF.class);
        when(etf.getId()).thenReturn(20L);
        when(etfService.findByIsin("IE0009HF1MK9")).thenReturn(Optional.of(etf));

        PortfolioPosition position = new PortfolioPosition();
        when(positionRepository.findByPortfolioIdAndAssetTypeAndAssetId(portfolioId, AssetType.ETF, 20L))
            .thenReturn(Optional.of(position));

        PortfolioImportResultDTO result = portfolioService.importPositionValues(portfolioId, file);

        assertThat(result.getUpdatedCount()).isEqualTo(1);
        assertThat(position.getQuantity()).isEqualByComparingTo("614");
    }

    @Test
    @DisplayName("skips values that round to zero to preserve quantity > 0 invariant")
    void skipsZeroValues() {
        Long portfolioId = 1L;
        when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(new Portfolio()));
        when(zeroCsvImporter.parseFile(file)).thenReturn(List.of(
            new PortfolioValueEntry("DE0005557508", BigDecimal.ZERO)
        ));

        Stock stock = mock(Stock.class);
        when(stock.getId()).thenReturn(10L);
        when(stockService.findByIsin("DE0005557508")).thenReturn(Optional.of(stock));

        PortfolioPosition position = new PortfolioPosition();
        when(positionRepository.findByPortfolioIdAndAssetTypeAndAssetId(portfolioId, AssetType.STOCK, 10L))
            .thenReturn(Optional.of(position));

        PortfolioImportResultDTO result = portfolioService.importPositionValues(portfolioId, file);

        assertThat(result.getUpdatedCount()).isEqualTo(0);
        assertThat(result.getWarnings()).isNotEmpty();
        verify(positionRepository, never()).save(any());
    }

    @Test
    @DisplayName("reports asset that exists but has no position as unmatched")
    void reportsAssetWithoutPositionAsUnmatched() {
        Long portfolioId = 1L;
        when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(new Portfolio()));
        when(zeroCsvImporter.parseFile(file)).thenReturn(List.of(
            new PortfolioValueEntry("DE0005557508", new BigDecimal("485"))
        ));

        Stock stock = mock(Stock.class);
        when(stock.getId()).thenReturn(10L);
        when(stockService.findByIsin("DE0005557508")).thenReturn(Optional.of(stock));
        when(positionRepository.findByPortfolioIdAndAssetTypeAndAssetId(portfolioId, AssetType.STOCK, 10L))
            .thenReturn(Optional.empty());

        PortfolioImportResultDTO result = portfolioService.importPositionValues(portfolioId, file);

        assertThat(result.getUpdatedCount()).isEqualTo(0);
        assertThat(result.getUnmatchedIsins()).containsExactly("DE0005557508");
    }
}
