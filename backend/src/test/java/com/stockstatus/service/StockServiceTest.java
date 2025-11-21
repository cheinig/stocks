package com.stockstatus.service;

import com.stockstatus.domain.Stock;
import com.stockstatus.exception.DuplicateStockException;
import com.stockstatus.exception.StockNotFoundException;
import com.stockstatus.repository.StockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for StockService
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StockService Unit Tests")
class StockServiceTest {

    @Mock
    private StockRepository stockRepository;

    @InjectMocks
    private StockServiceImpl stockService;

    private Stock testStock;

    @BeforeEach
    void setUp() {
        testStock = Stock.builder()
            .id(1L)
            .name("Apple Inc.")
            .isin("US0378331005")
            .country("US")
            .sector("Technology")
            .build();
    }

    @Test
    @DisplayName("Should create stock successfully")
    void createStock_Success() {
        // Given
        Stock newStock = Stock.builder()
            .name("Apple Inc.")
            .isin("US0378331005")
            .country("US")
            .sector("Technology")
            .build();

        when(stockRepository.existsByIsin(newStock.getIsin())).thenReturn(false);
        when(stockRepository.save(any(Stock.class))).thenReturn(testStock);

        // When
        Stock result = stockService.createStock(newStock);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getIsin()).isEqualTo("US0378331005");
        verify(stockRepository).existsByIsin("US0378331005");
        verify(stockRepository).save(newStock);
    }

    @Test
    @DisplayName("Should throw DuplicateStockException when ISIN already exists")
    void createStock_DuplicateIsin_ThrowsException() {
        // Given
        Stock newStock = Stock.builder()
            .name("Apple Inc.")
            .isin("US0378331005")
            .country("US")
            .sector("Technology")
            .build();

        when(stockRepository.existsByIsin(newStock.getIsin())).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> stockService.createStock(newStock))
            .isInstanceOf(DuplicateStockException.class)
            .hasMessageContaining("US0378331005");

        verify(stockRepository).existsByIsin("US0378331005");
        verify(stockRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for invalid ISIN format")
    void createStock_InvalidIsin_ThrowsException() {
        // Given
        Stock newStock = Stock.builder()
            .name("Test Stock")
            .isin("INVALID")
            .country("US")
            .sector("Technology")
            .build();

        // When/Then
        assertThatThrownBy(() -> stockService.createStock(newStock))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid ISIN format");

        verify(stockRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should update stock successfully")
    void updateStock_Success() {
        // Given
        Stock updateData = Stock.builder()
            .name("Apple Inc. Updated")
            .isin("US0378331005")
            .country("US")
            .sector("Technology")
            .build();

        when(stockRepository.findById(1L)).thenReturn(Optional.of(testStock));
        when(stockRepository.save(any(Stock.class))).thenReturn(testStock);

        // When
        Stock result = stockService.updateStock(1L, updateData);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Apple Inc. Updated");
        verify(stockRepository).findById(1L);
        verify(stockRepository).save(testStock);
    }

    @Test
    @DisplayName("Should throw StockNotFoundException when updating non-existent stock")
    void updateStock_NotFound_ThrowsException() {
        // Given
        Stock updateData = Stock.builder()
            .name("Test")
            .isin("US0378331005")
            .country("US")
            .sector("Technology")
            .build();

        when(stockRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> stockService.updateStock(999L, updateData))
            .isInstanceOf(StockNotFoundException.class);

        verify(stockRepository).findById(999L);
        verify(stockRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw DuplicateStockException when updating with existing ISIN")
    void updateStock_DuplicateIsin_ThrowsException() {
        // Given
        Stock updateData = Stock.builder()
            .name("Apple Inc.")
            .isin("DE0005140008") // Different ISIN
            .country("US")
            .sector("Technology")
            .build();

        when(stockRepository.findById(1L)).thenReturn(Optional.of(testStock));
        when(stockRepository.existsByIsin("DE0005140008")).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> stockService.updateStock(1L, updateData))
            .isInstanceOf(DuplicateStockException.class)
            .hasMessageContaining("DE0005140008");

        verify(stockRepository).findById(1L);
        verify(stockRepository).existsByIsin("DE0005140008");
        verify(stockRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should delete stock successfully")
    void deleteStock_Success() {
        // Given
        when(stockRepository.existsById(1L)).thenReturn(true);
        doNothing().when(stockRepository).deleteById(1L);

        // When
        stockService.deleteStock(1L);

        // Then
        verify(stockRepository).existsById(1L);
        verify(stockRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Should throw StockNotFoundException when deleting non-existent stock")
    void deleteStock_NotFound_ThrowsException() {
        // Given
        when(stockRepository.existsById(999L)).thenReturn(false);

        // When/Then
        assertThatThrownBy(() -> stockService.deleteStock(999L))
            .isInstanceOf(StockNotFoundException.class);

        verify(stockRepository).existsById(999L);
        verify(stockRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Should find stock by ID")
    void findById_Success() {
        // Given
        when(stockRepository.findById(1L)).thenReturn(Optional.of(testStock));

        // When
        Stock result = stockService.findById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Apple Inc.");
        verify(stockRepository).findById(1L);
    }

    @Test
    @DisplayName("Should throw StockNotFoundException when stock not found by ID")
    void findById_NotFound_ThrowsException() {
        // Given
        when(stockRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> stockService.findById(999L))
            .isInstanceOf(StockNotFoundException.class);

        verify(stockRepository).findById(999L);
    }

    @Test
    @DisplayName("Should find stock by ISIN")
    void findByIsin_Success() {
        // Given
        when(stockRepository.findByIsin("US0378331005")).thenReturn(Optional.of(testStock));

        // When
        Optional<Stock> result = stockService.findByIsin("US0378331005");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getIsin()).isEqualTo("US0378331005");
        verify(stockRepository).findByIsin("US0378331005");
    }

    @Test
    @DisplayName("Should find all stocks with pagination")
    void findAll_WithPagination() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<Stock> stocks = Arrays.asList(testStock);
        Page<Stock> page = new PageImpl<>(stocks, pageable, 1);

        when(stockRepository.findAll(pageable)).thenReturn(page);

        // When
        Page<Stock> result = stockService.findAll(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(stockRepository).findAll(pageable);
    }

    @Test
    @DisplayName("Should find stocks by country")
    void findByCountry_Success() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<Stock> stocks = Arrays.asList(testStock);
        Page<Stock> page = new PageImpl<>(stocks, pageable, 1);

        when(stockRepository.findByCountry("US", pageable)).thenReturn(page);

        // When
        Page<Stock> result = stockService.findByCountry("US", pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCountry()).isEqualTo("US");
        verify(stockRepository).findByCountry("US", pageable);
    }

    @Test
    @DisplayName("Should search stocks by name or ISIN")
    void searchByNameOrIsin_Success() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<Stock> stocks = Arrays.asList(testStock);
        Page<Stock> page = new PageImpl<>(stocks, pageable, 1);

        when(stockRepository.searchByNameOrIsin("Apple", pageable)).thenReturn(page);

        // When
        Page<Stock> result = stockService.searchByNameOrIsin("Apple", pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(stockRepository).searchByNameOrIsin("Apple", pageable);
    }

    @Test
    @DisplayName("Should check if stock exists by ISIN")
    void existsByIsin_ReturnsTrue() {
        // Given
        when(stockRepository.existsByIsin("US0378331005")).thenReturn(true);

        // When
        boolean result = stockService.existsByIsin("US0378331005");

        // Then
        assertThat(result).isTrue();
        verify(stockRepository).existsByIsin("US0378331005");
    }

    @Test
    @DisplayName("Should count all stocks")
    void count_Success() {
        // Given
        when(stockRepository.count()).thenReturn(10L);

        // When
        long result = stockService.count();

        // Then
        assertThat(result).isEqualTo(10L);
        verify(stockRepository).count();
    }

    @Test
    @DisplayName("Should count stocks by country")
    void countByCountry_Success() {
        // Given
        when(stockRepository.countByCountry("US")).thenReturn(5L);

        // When
        long result = stockService.countByCountry("US");

        // Then
        assertThat(result).isEqualTo(5L);
        verify(stockRepository).countByCountry("US");
    }
}
