package com.stockstatus.service;

import com.stockstatus.domain.ETF;
import com.stockstatus.domain.ETFAllocation;
import com.stockstatus.domain.ImporterType;
import com.stockstatus.domain.Stock;
import com.stockstatus.exception.DuplicateETFException;
import com.stockstatus.exception.ETFNotFoundException;
import com.stockstatus.repository.ETFAllocationRepository;
import com.stockstatus.repository.ETFRepository;
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

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ETFService
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ETFService Unit Tests")
class ETFServiceTest {

    @Mock
    private ETFRepository etfRepository;

    @Mock
    private ETFAllocationRepository etfAllocationRepository;

    @InjectMocks
    private ETFServiceImpl etfService;

    private ETF testETF;
    private Stock testStock;
    private ETFAllocation testAllocation;

    @BeforeEach
    void setUp() {
        testETF = ETF.builder()
            .id(1L)
            .name("Vanguard S&P 500")
            .isin("IE00B3XXRP09")
            .importerType(ImporterType.FIDELITY)
            .build();

        testStock = Stock.builder()
            .id(1L)
            .name("Apple Inc.")
            .isin("US0378331005")
            .country("US")
            .sector("Technology")
            .build();

        testAllocation = ETFAllocation.builder()
            .id(1L)
            .etf(testETF)
            .stock(testStock)
            .percentage(new BigDecimal("10.50"))
            .allocationVersion(1)
            .build();
    }

    @Test
    @DisplayName("Should create ETF successfully")
    void createETF_Success() {
        // Given
        ETF newETF = ETF.builder()
            .name("Vanguard S&P 500")
            .isin("IE00B3XXRP09")
            .importerType(ImporterType.FIDELITY)
            .build();

        when(etfRepository.existsByIsin(newETF.getIsin())).thenReturn(false);
        when(etfRepository.save(any(ETF.class))).thenReturn(testETF);

        // When
        ETF result = etfService.createETF(newETF);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getIsin()).isEqualTo("IE00B3XXRP09");
        verify(etfRepository).existsByIsin("IE00B3XXRP09");
        verify(etfRepository).save(newETF);
    }

    @Test
    @DisplayName("Should throw DuplicateETFException when ISIN already exists")
    void createETF_DuplicateIsin_ThrowsException() {
        // Given
        ETF newETF = ETF.builder()
            .name("Vanguard S&P 500")
            .isin("IE00B3XXRP09")
            .importerType(ImporterType.FIDELITY)
            .build();

        when(etfRepository.existsByIsin(newETF.getIsin())).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> etfService.createETF(newETF))
            .isInstanceOf(DuplicateETFException.class)
            .hasMessageContaining("IE00B3XXRP09");

        verify(etfRepository).existsByIsin("IE00B3XXRP09");
        verify(etfRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for invalid ISIN format")
    void createETF_InvalidIsin_ThrowsException() {
        // Given
        ETF newETF = ETF.builder()
            .name("Test ETF")
            .isin("INVALID")
            .importerType(ImporterType.FIDELITY)
            .build();

        // When/Then
        assertThatThrownBy(() -> etfService.createETF(newETF))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid ISIN format");

        verify(etfRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should update ETF successfully")
    void updateETF_Success() {
        // Given
        ETF updateData = ETF.builder()
            .name("Vanguard S&P 500 Updated")
            .isin("IE00B3XXRP09")
            .importerType(ImporterType.XTRACKERS)
            .build();

        when(etfRepository.findById(1L)).thenReturn(Optional.of(testETF));
        when(etfRepository.save(any(ETF.class))).thenReturn(testETF);

        // When
        ETF result = etfService.updateETF(1L, updateData);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Vanguard S&P 500 Updated");
        verify(etfRepository).findById(1L);
        verify(etfRepository).save(testETF);
    }

    @Test
    @DisplayName("Should throw ETFNotFoundException when updating non-existent ETF")
    void updateETF_NotFound_ThrowsException() {
        // Given
        ETF updateData = ETF.builder()
            .name("Test")
            .isin("IE00B3XXRP09")
            .importerType(ImporterType.FIDELITY)
            .build();

        when(etfRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> etfService.updateETF(999L, updateData))
            .isInstanceOf(ETFNotFoundException.class);

        verify(etfRepository).findById(999L);
        verify(etfRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should delete ETF successfully")
    void deleteETF_Success() {
        // Given
        when(etfRepository.existsById(1L)).thenReturn(true);
        doNothing().when(etfRepository).deleteById(1L);

        // When
        etfService.deleteETF(1L);

        // Then
        verify(etfRepository).existsById(1L);
        verify(etfRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Should throw ETFNotFoundException when deleting non-existent ETF")
    void deleteETF_NotFound_ThrowsException() {
        // Given
        when(etfRepository.existsById(999L)).thenReturn(false);

        // When/Then
        assertThatThrownBy(() -> etfService.deleteETF(999L))
            .isInstanceOf(ETFNotFoundException.class);

        verify(etfRepository).existsById(999L);
        verify(etfRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Should find ETF by ID")
    void findById_Success() {
        // Given
        when(etfRepository.findById(1L)).thenReturn(Optional.of(testETF));

        // When
        ETF result = etfService.findById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Vanguard S&P 500");
        verify(etfRepository).findById(1L);
    }

    @Test
    @DisplayName("Should throw ETFNotFoundException when ETF not found by ID")
    void findById_NotFound_ThrowsException() {
        // Given
        when(etfRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> etfService.findById(999L))
            .isInstanceOf(ETFNotFoundException.class);

        verify(etfRepository).findById(999L);
    }

    @Test
    @DisplayName("Should find ETF by ISIN")
    void findByIsin_Success() {
        // Given
        when(etfRepository.findByIsin("IE00B3XXRP09")).thenReturn(Optional.of(testETF));

        // When
        Optional<ETF> result = etfService.findByIsin("IE00B3XXRP09");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getIsin()).isEqualTo("IE00B3XXRP09");
        verify(etfRepository).findByIsin("IE00B3XXRP09");
    }

    @Test
    @DisplayName("Should find all ETFs with pagination")
    void findAll_WithPagination() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<ETF> etfs = Arrays.asList(testETF);
        Page<ETF> page = new PageImpl<>(etfs, pageable, 1);

        when(etfRepository.findAll(pageable)).thenReturn(page);

        // When
        Page<ETF> result = etfService.findAll(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(etfRepository).findAll(pageable);
    }

    @Test
    @DisplayName("Should find ETFs by importer type")
    void findByImporterType_Success() {
        // Given
        List<ETF> etfs = Arrays.asList(testETF);
        when(etfRepository.findByImporterType(ImporterType.FIDELITY)).thenReturn(etfs);

        // When
        List<ETF> result = etfService.findByImporterType(ImporterType.FIDELITY);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getImporterType()).isEqualTo(ImporterType.FIDELITY);
        verify(etfRepository).findByImporterType(ImporterType.FIDELITY);
    }

    @Test
    @DisplayName("Should search ETFs by name or ISIN")
    void searchByNameOrIsin_Success() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<ETF> etfs = Arrays.asList(testETF);
        Page<ETF> page = new PageImpl<>(etfs, pageable, 1);

        when(etfRepository.searchByNameOrIsin("Vanguard", pageable)).thenReturn(page);

        // When
        Page<ETF> result = etfService.searchByNameOrIsin("Vanguard", pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(etfRepository).searchByNameOrIsin("Vanguard", pageable);
    }

    @Test
    @DisplayName("Should get current allocation for ETF")
    void getCurrentAllocation_Success() {
        // Given
        List<ETFAllocation> allocations = Arrays.asList(testAllocation);
        when(etfRepository.findById(1L)).thenReturn(Optional.of(testETF));
        when(etfAllocationRepository.findLatestByEtfId(1L)).thenReturn(allocations);

        // When
        List<ETFAllocation> result = etfService.getCurrentAllocation(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPercentage()).isEqualByComparingTo(new BigDecimal("10.50"));
        verify(etfRepository).findById(1L);
        verify(etfAllocationRepository).findLatestByEtfId(1L);
    }

    @Test
    @DisplayName("Should throw ETFNotFoundException when getting allocation for non-existent ETF")
    void getCurrentAllocation_ETFNotFound_ThrowsException() {
        // Given
        when(etfRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> etfService.getCurrentAllocation(999L))
            .isInstanceOf(ETFNotFoundException.class);

        verify(etfRepository).findById(999L);
        verify(etfAllocationRepository, never()).findLatestByEtfId(any());
    }

    @Test
    @DisplayName("Should get allocation history")
    void getAllocationHistory_Success() {
        // Given
        List<Integer> versions = Arrays.asList(1, 2);
        List<ETFAllocation> allocations = Arrays.asList(testAllocation);

        when(etfRepository.findById(1L)).thenReturn(Optional.of(testETF));
        when(etfAllocationRepository.findAllVersionsByEtfId(1L)).thenReturn(versions);
        when(etfAllocationRepository.findByEtfIdAndAllocationVersion(1L, 1)).thenReturn(allocations);
        when(etfAllocationRepository.findByEtfIdAndAllocationVersion(1L, 2)).thenReturn(allocations);

        // When
        Map<Integer, List<ETFAllocation>> result = etfService.getAllocationHistory(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).containsKeys(1, 2);
        verify(etfRepository).findById(1L);
        verify(etfAllocationRepository).findAllVersionsByEtfId(1L);
    }

    @Test
    @DisplayName("Should get allocation versions")
    void getAllocationVersions_Success() {
        // Given
        List<Integer> versions = Arrays.asList(1, 2, 3);
        when(etfRepository.findById(1L)).thenReturn(Optional.of(testETF));
        when(etfAllocationRepository.findAllVersionsByEtfId(1L)).thenReturn(versions);

        // When
        List<Integer> result = etfService.getAllocationVersions(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);
        assertThat(result).containsExactly(1, 2, 3);
        verify(etfRepository).findById(1L);
        verify(etfAllocationRepository).findAllVersionsByEtfId(1L);
    }

    @Test
    @DisplayName("Should check if ETF has allocations")
    void hasAllocations_ReturnsTrue() {
        // Given
        when(etfAllocationRepository.existsByEtfId(1L)).thenReturn(true);

        // When
        boolean result = etfService.hasAllocations(1L);

        // Then
        assertThat(result).isTrue();
        verify(etfAllocationRepository).existsByEtfId(1L);
    }

    @Test
    @DisplayName("Should count all ETFs")
    void count_Success() {
        // Given
        when(etfRepository.count()).thenReturn(5L);

        // When
        long result = etfService.count();

        // Then
        assertThat(result).isEqualTo(5L);
        verify(etfRepository).count();
    }

    @Test
    @DisplayName("Should count ETFs by importer type")
    void countByImporterType_Success() {
        // Given
        when(etfRepository.countByImporterType(ImporterType.FIDELITY)).thenReturn(3L);

        // When
        long result = etfService.countByImporterType(ImporterType.FIDELITY);

        // Then
        assertThat(result).isEqualTo(3L);
        verify(etfRepository).countByImporterType(ImporterType.FIDELITY);
    }
}
