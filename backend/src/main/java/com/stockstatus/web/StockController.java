package com.stockstatus.web;

import com.stockstatus.domain.Stock;
import com.stockstatus.dto.StockRequestDTO;
import com.stockstatus.dto.StockResponseDTO;
import com.stockstatus.service.StockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

/**
 * REST Controller for Stock operations
 */
@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Stocks", description = "Stock management APIs")
public class StockController {

    private final StockService stockService;

    /**
     * Create a new stock
     * POST /api/stocks
     */
    @PostMapping
    @Operation(summary = "Create a new stock", description = "Creates a new stock with the provided details")
    public ResponseEntity<StockResponseDTO> createStock(@Valid @RequestBody StockRequestDTO request) {
        log.info("REST request to create Stock: {}", request.getName());

        Stock stock = Stock.builder()
            .name(request.getName())
            .isin(request.getIsin())
            .country(request.getCountry())
            .sector(request.getSector())
            .build();

        Stock createdStock = stockService.createStock(stock);
        StockResponseDTO response = StockResponseDTO.fromEntity(createdStock);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all stocks with pagination
     * GET /api/stocks?page=0&size=20&sort=name,asc
     */
    @GetMapping
    public ResponseEntity<Page<StockResponseDTO>> getAllStocks(
        @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        log.debug("REST request to get all Stocks, page: {}", pageable.getPageNumber());

        Page<Stock> stocks = stockService.findAll(pageable);
        Page<StockResponseDTO> response = stocks.map(StockResponseDTO::fromEntity);

        return ResponseEntity.ok(response);
    }

    /**
     * Get a stock by ID
     * GET /api/stocks/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<StockResponseDTO> getStockById(@PathVariable Long id) {
        log.debug("REST request to get Stock by ID: {}", id);

        Stock stock = stockService.findById(id);
        StockResponseDTO response = StockResponseDTO.fromEntity(stock);

        return ResponseEntity.ok(response);
    }

    /**
     * Update a stock
     * PUT /api/stocks/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<StockResponseDTO> updateStock(
        @PathVariable Long id,
        @Valid @RequestBody StockRequestDTO request
    ) {
        log.info("REST request to update Stock ID: {}", id);

        Stock stock = Stock.builder()
            .name(request.getName())
            .isin(request.getIsin())
            .country(request.getCountry())
            .sector(request.getSector())
            .build();

        Stock updatedStock = stockService.updateStock(id, stock);
        StockResponseDTO response = StockResponseDTO.fromEntity(updatedStock);

        return ResponseEntity.ok(response);
    }

    /**
     * Delete a stock
     * DELETE /api/stocks/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStock(@PathVariable Long id) {
        log.info("REST request to delete Stock ID: {}", id);

        stockService.deleteStock(id);

        return ResponseEntity.noContent().build();
    }

    /**
     * Search stocks by name or ISIN
     * GET /api/stocks/search?query=AAPL&page=0&size=20
     */
    @GetMapping("/search")
    public ResponseEntity<Page<StockResponseDTO>> searchStocks(
        @RequestParam String query,
        @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        log.debug("REST request to search Stocks with query: {}", query);

        Page<Stock> stocks = stockService.searchByNameOrIsin(query, pageable);
        Page<StockResponseDTO> response = stocks.map(StockResponseDTO::fromEntity);

        return ResponseEntity.ok(response);
    }

    /**
     * Get stocks by country
     * GET /api/stocks/by-country/{country}?page=0&size=20
     */
    @GetMapping("/by-country/{country}")
    public ResponseEntity<Page<StockResponseDTO>> getStocksByCountry(
        @PathVariable String country,
        @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        log.debug("REST request to get Stocks by country: {}", country);

        Page<Stock> stocks = stockService.findByCountry(country, pageable);
        Page<StockResponseDTO> response = stocks.map(StockResponseDTO::fromEntity);

        return ResponseEntity.ok(response);
    }
}
