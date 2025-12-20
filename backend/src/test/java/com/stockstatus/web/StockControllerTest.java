package com.stockstatus.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockstatus.domain.Stock;
import com.stockstatus.dto.StockRequestDTO;
import com.stockstatus.dto.StockResponseDTO;
import com.stockstatus.repository.ETFAllocationRepository;
import com.stockstatus.repository.PortfolioPositionRepository;
import com.stockstatus.service.StockService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * API tests for StockController using MockMvc
 */
@WebMvcTest(StockController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("StockController API Tests")
class StockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private StockService stockService;

    @MockBean
    private ETFAllocationRepository etfAllocationRepository;

    @MockBean
    private PortfolioPositionRepository portfolioPositionRepository;

    @Test
    @DisplayName("POST /api/stocks - Create stock successfully")
    void createStock_Success() throws Exception {
        // Given
        StockRequestDTO request = StockRequestDTO.builder()
            .name("Apple Inc.")
            .isin("US0378331005")
            .country("US")
            .sector("Technology")
            .build();

        Stock savedStock = Stock.builder()
            .id(1L)
            .name("Apple Inc.")
            .isin("US0378331005")
            .country("US")
            .sector("Technology")
            .build();

        when(stockService.createStock(any(Stock.class))).thenReturn(savedStock);

        // When/Then
        mockMvc.perform(post("/api/stocks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Apple Inc."))
            .andExpect(jsonPath("$.isin").value("US0378331005"));

        verify(stockService).createStock(any(Stock.class));
    }

    @Test
    @DisplayName("POST /api/stocks - Validation error for empty name")
    void createStock_ValidationError() throws Exception {
        // Given
        StockRequestDTO request = StockRequestDTO.builder()
            .name("")
            .isin("US0378331005")
            .country("US")
            .sector("Technology")
            .build();

        // When/Then
        mockMvc.perform(post("/api/stocks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());

        verify(stockService, never()).createStock(any());
    }

    @Test
    @DisplayName("GET /api/stocks - Get all stocks with pagination")
    void getAllStocks_Success() throws Exception {
        // Given
        Stock stock = Stock.builder()
            .id(1L)
            .name("Apple Inc.")
            .isin("US0378331005")
            .country("US")
            .sector("Technology")
            .build();

        List<Stock> stocks = Arrays.asList(stock);
        Page<Stock> page = new PageImpl<>(stocks, PageRequest.of(0, 20), 1);

        when(stockService.findAll(any(PageRequest.class))).thenReturn(page);

        // When/Then
        mockMvc.perform(get("/api/stocks")
                .param("page", "0")
                .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value(1))
            .andExpect(jsonPath("$.content[0].name").value("Apple Inc."))
            .andExpect(jsonPath("$.totalElements").value(1));

        verify(stockService).findAll(any(PageRequest.class));
    }

    @Test
    @DisplayName("GET /api/stocks/{id} - Get stock by ID")
    void getStockById_Success() throws Exception {
        // Given
        Stock stock = Stock.builder()
            .id(1L)
            .name("Apple Inc.")
            .isin("US0378331005")
            .country("US")
            .sector("Technology")
            .build();

        when(stockService.findById(1L)).thenReturn(stock);

        // When/Then
        mockMvc.perform(get("/api/stocks/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Apple Inc."));

        verify(stockService).findById(1L);
    }

    @Test
    @DisplayName("PUT /api/stocks/{id} - Update stock successfully")
    void updateStock_Success() throws Exception {
        // Given
        StockRequestDTO request = StockRequestDTO.builder()
            .name("Apple Inc. Updated")
            .isin("US0378331005")
            .country("US")
            .sector("Technology")
            .build();

        Stock updatedStock = Stock.builder()
            .id(1L)
            .name("Apple Inc. Updated")
            .isin("US0378331005")
            .country("US")
            .sector("Technology")
            .build();

        when(stockService.updateStock(eq(1L), any(Stock.class))).thenReturn(updatedStock);

        // When/Then
        mockMvc.perform(put("/api/stocks/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Apple Inc. Updated"));

        verify(stockService).updateStock(eq(1L), any(Stock.class));
    }

    @Test
    @DisplayName("DELETE /api/stocks/{id} - Delete stock successfully")
    void deleteStock_Success() throws Exception {
        // Given
        doNothing().when(stockService).deleteStock(1L);

        // When/Then
        mockMvc.perform(delete("/api/stocks/1"))
            .andExpect(status().isNoContent());

        verify(stockService).deleteStock(1L);
    }

    @Test
    @DisplayName("GET /api/stocks/search - Search stocks by query")
    void searchStocks_Success() throws Exception {
        // Given
        Stock stock = Stock.builder()
            .id(1L)
            .name("Apple Inc.")
            .isin("US0378331005")
            .country("US")
            .sector("Technology")
            .build();

        List<Stock> stocks = Arrays.asList(stock);
        Page<Stock> page = new PageImpl<>(stocks, PageRequest.of(0, 20), 1);

        when(stockService.searchByNameOrIsin(eq("Apple"), any(PageRequest.class))).thenReturn(page);

        // When/Then
        mockMvc.perform(get("/api/stocks/search")
                .param("query", "Apple")
                .param("page", "0")
                .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].name").value("Apple Inc."));

        verify(stockService).searchByNameOrIsin(eq("Apple"), any(PageRequest.class));
    }
}
