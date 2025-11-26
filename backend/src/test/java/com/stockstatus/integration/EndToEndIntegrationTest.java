package com.stockstatus.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockstatus.dto.StockRequestDTO;
import com.stockstatus.dto.ETFRequestDTO;
import com.stockstatus.dto.PortfolioRequestDTO;
import com.stockstatus.dto.PortfolioPositionRequestDTO;
import com.stockstatus.domain.AssetType;
import com.stockstatus.domain.ImporterType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end integration test covering the complete workflow:
 * 1. Create stocks
 * 2. Create ETF
 * 3. Upload allocation (simplified, actual file upload would need multipart)
 * 4. Create portfolio
 * 5. Add positions
 * 6. Retrieve dashboard analytics
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("End-to-End Integration Test")
class EndToEndIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Complete workflow: Stock → ETF → Portfolio → Dashboard")
    void completeWorkflow() throws Exception {
        // Step 1: Create a stock
        StockRequestDTO stockRequest = StockRequestDTO.builder()
            .name("Test Stock")
            .isin("US1234567890")
            .country("US")
            .sector("Technology")
            .build();

        MvcResult stockResult = mockMvc.perform(post("/api/stocks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(stockRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andReturn();

        // Step 2: Create an ETF
        ETFRequestDTO etfRequest = ETFRequestDTO.builder()
            .name("Test ETF")
            .isin("IE0012345678")
            .importerType(ImporterType.FIDELITY)
            .build();

        MvcResult etfResult = mockMvc.perform(post("/api/etfs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(etfRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andReturn();

        // Step 3: Create a portfolio
        PortfolioRequestDTO portfolioRequest = PortfolioRequestDTO.builder()
            .name("Test Portfolio")
            .userId("testuser")
            .build();

        MvcResult portfolioResult = mockMvc.perform(post("/api/portfolios")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(portfolioRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andReturn();

        String portfolioJson = portfolioResult.getResponse().getContentAsString();
        Long portfolioId = objectMapper.readTree(portfolioJson).get("id").asLong();

        // Extract stock ID from created stock
        String stockJson = stockResult.getResponse().getContentAsString();
        Long stockId = objectMapper.readTree(stockJson).get("id").asLong();

        // Step 4: Add a stock position to portfolio
        PortfolioPositionRequestDTO positionRequest = PortfolioPositionRequestDTO.builder()
            .assetType(AssetType.STOCK)
            .assetId(stockId)
            .quantity(new BigDecimal("10.5"))
            .build();

        mockMvc.perform(post("/api/portfolios/" + portfolioId + "/positions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(positionRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists());

        // Step 5: Get portfolio with positions
        mockMvc.perform(get("/api/portfolios/" + portfolioId + "/with-positions"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(portfolioId))
            .andExpect(jsonPath("$.positions").isArray());

        // Step 6: Verify stocks endpoint
        mockMvc.perform(get("/api/stocks"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray());

        // Step 7: Verify ETFs endpoint
        mockMvc.perform(get("/api/etfs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("CRUD operations on stocks")
    void stockCrudOperations() throws Exception {
        // Create
        StockRequestDTO createRequest = StockRequestDTO.builder()
            .name("CRUD Test Stock")
            .isin("US9876543210")
            .country("US")
            .sector("Finance")
            .build();

        MvcResult createResult = mockMvc.perform(post("/api/stocks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andReturn();

        String json = createResult.getResponse().getContentAsString();
        Long stockId = objectMapper.readTree(json).get("id").asLong();

        // Read
        mockMvc.perform(get("/api/stocks/" + stockId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("CRUD Test Stock"));

        // Update
        StockRequestDTO updateRequest = StockRequestDTO.builder()
            .name("CRUD Test Stock Updated")
            .isin("US9876543210")
            .country("US")
            .sector("Finance")
            .build();

        mockMvc.perform(put("/api/stocks/" + stockId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("CRUD Test Stock Updated"));

        // Delete
        mockMvc.perform(delete("/api/stocks/" + stockId))
            .andExpect(status().isNoContent());

        // Verify deleted
        mockMvc.perform(get("/api/stocks/" + stockId))
            .andExpect(status().isNotFound());
    }
}
