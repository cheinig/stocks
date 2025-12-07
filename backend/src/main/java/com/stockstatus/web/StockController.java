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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    /**
     * Upload logo for a stock
     * POST /api/stocks/{id}/logo
     */
    @PostMapping(value = "/{id}/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload logo for a stock")
    public ResponseEntity<Void> uploadLogo(
        @PathVariable Long id,
        @RequestParam("file") MultipartFile file
    ) {
        log.info("REST request to upload logo for Stock ID: {}", id);

        try {
            Stock stock = stockService.findById(id);
            stock.setLogo(file.getBytes());
            stock.setLogoContentType(file.getContentType());
            stockService.updateStock(id, stock);

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error uploading logo for stock {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get logo for a stock
     * GET /api/stocks/{id}/logo
     */
    @GetMapping("/{id}/logo")
    @Operation(summary = "Get logo for a stock")
    public ResponseEntity<byte[]> getLogo(@PathVariable Long id) {
        log.debug("REST request to get logo for Stock ID: {}", id);

        Stock stock = stockService.findById(id);

        if (stock.getLogo() == null || stock.getLogo().length == 0) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(stock.getLogoContentType() != null ? stock.getLogoContentType() : "image/png"))
            .body(stock.getLogo());
    }

    /**
     * Fetch and store logo from elbstream API
     * POST /api/stocks/{id}/logo/fetch
     */
    @PostMapping("/{id}/logo/fetch")
    @Operation(summary = "Fetch logo from elbstream API and store it")
    public ResponseEntity<Void> fetchAndStoreLogo(@PathVariable Long id) {
        log.info("REST request to fetch and store logo for Stock ID: {}", id);

        try {
            Stock stock = stockService.findById(id);
            String logoUrl = "https://api.elbstream.com/logos/isin/" + stock.getIsin();

            // Fetch logo from elbstream API
            java.net.URI uri = new java.net.URI(logoUrl);
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) uri.toURL().openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                byte[] logoBytes = connection.getInputStream().readAllBytes();
                String contentType = connection.getContentType();

                // Verwende die neue updateLogo-Methode
                stockService.updateLogo(id, logoBytes, contentType != null ? contentType : "image/png");

                log.info("Successfully fetched and stored logo for stock {}, size: {} bytes", id, logoBytes.length);
                return ResponseEntity.ok().build();
            } else {
                log.warn("Failed to fetch logo for stock {}: HTTP {}", id, responseCode);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
        } catch (Exception e) {
            log.error("Error fetching logo for stock {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
