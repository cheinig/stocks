package com.stockstatus.web;

import com.stockstatus.domain.ETF;
import com.stockstatus.domain.ETFAllocation;
import com.stockstatus.domain.Stock;
import com.stockstatus.dto.*;
import com.stockstatus.service.ETFService;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * REST Controller for ETF operations
 */
@RestController
@RequestMapping("/api/etfs")
@RequiredArgsConstructor
@Slf4j
public class ETFController {

    private final ETFService etfService;

    /**
     * Create a new ETF
     * POST /api/etfs
     */
    @PostMapping
    public ResponseEntity<ETFResponseDTO> createETF(@Valid @RequestBody ETFRequestDTO request) {
        log.info("REST request to create ETF: {}", request.getName());

        ETF etf = ETF.builder()
            .name(request.getName())
            .isin(request.getIsin())
            .importerType(request.getImporterType())
            .webUrl(request.getWebUrl())
            .webDataId(request.getWebDataId())
            .tickerSymbol(request.getTickerSymbol())
            .build();

        ETF createdETF = etfService.createETF(etf);
        ETFResponseDTO response = ETFResponseDTO.fromEntity(createdETF);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all ETFs with pagination
     * GET /api/etfs?page=0&size=20&sort=name,asc
     */
    @GetMapping
    public ResponseEntity<Page<ETFResponseDTO>> getAllETFs(
        @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        log.debug("REST request to get all ETFs, page: {}", pageable.getPageNumber());

        Page<ETF> etfs = etfService.findAll(pageable);
        Page<ETFResponseDTO> response = etfs.map(ETFResponseDTO::fromEntity);

        return ResponseEntity.ok(response);
    }

    /**
     * Get an ETF by ID
     * GET /api/etfs/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ETFResponseDTO> getETFById(@PathVariable Long id) {
        log.debug("REST request to get ETF by ID: {}", id);

        ETF etf = etfService.findById(id);
        ETFResponseDTO response = ETFResponseDTO.fromEntity(etf);

        return ResponseEntity.ok(response);
    }

    /**
     * Update an ETF
     * PUT /api/etfs/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ETFResponseDTO> updateETF(
        @PathVariable Long id,
        @Valid @RequestBody ETFRequestDTO request
    ) {
        log.info("REST request to update ETF ID: {}", id);

        ETF etf = ETF.builder()
            .name(request.getName())
            .isin(request.getIsin())
            .importerType(request.getImporterType())
            .webUrl(request.getWebUrl())
            .webDataId(request.getWebDataId())
            .tickerSymbol(request.getTickerSymbol())
            .build();

        ETF updatedETF = etfService.updateETF(id, etf);
        ETFResponseDTO response = ETFResponseDTO.fromEntity(updatedETF);

        return ResponseEntity.ok(response);
    }

    /**
     * Delete an ETF
     * DELETE /api/etfs/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteETF(@PathVariable Long id) {
        log.info("REST request to delete ETF ID: {}", id);

        etfService.deleteETF(id);

        return ResponseEntity.noContent().build();
    }

    /**
     * Get current allocation for an ETF
     * GET /api/etfs/{id}/allocations
     */
    @GetMapping("/{id}/allocations")
    public ResponseEntity<List<ETFAllocationResponseDTO>> getCurrentAllocation(@PathVariable Long id) {
        log.debug("REST request to get current allocation for ETF ID: {}", id);

        List<ETFAllocation> allocations = etfService.getCurrentAllocation(id);
        List<ETFAllocationResponseDTO> response = allocations.stream()
            .map(ETFAllocationResponseDTO::fromEntity)
            .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Get allocation history for an ETF
     * GET /api/etfs/{id}/allocations/history
     * Returns a map of version numbers to allocation lists
     */
    @GetMapping("/{id}/allocations/history")
    public ResponseEntity<Map<Integer, List<ETFAllocationResponseDTO>>> getAllocationHistory(@PathVariable Long id) {
        log.debug("REST request to get allocation history for ETF ID: {}", id);

        Map<Integer, List<ETFAllocation>> history = etfService.getAllocationHistory(id);

        Map<Integer, List<ETFAllocationResponseDTO>> response = history.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().stream()
                    .map(ETFAllocationResponseDTO::fromEntity)
                    .collect(Collectors.toList())
            ));

        return ResponseEntity.ok(response);
    }

    /**
     * Get all allocation versions for an ETF
     * GET /api/etfs/{id}/allocations/versions
     * Returns a list of version numbers
     */
    @GetMapping("/{id}/allocations/versions")
    public ResponseEntity<List<Integer>> getAllocationVersions(@PathVariable Long id) {
        log.debug("REST request to get allocation versions for ETF ID: {}", id);

        List<Integer> versions = etfService.getAllocationVersions(id);

        return ResponseEntity.ok(versions);
    }

    /**
     * Search ETFs by name or ISIN
     * GET /api/etfs/search?query=MSCI&page=0&size=20
     */
    @GetMapping("/search")
    public ResponseEntity<Page<ETFResponseDTO>> searchETFs(
        @RequestParam String query,
        @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        log.debug("REST request to search ETFs with query: {}", query);

        Page<ETF> etfs = etfService.searchByNameOrIsin(query, pageable);
        Page<ETFResponseDTO> response = etfs.map(ETFResponseDTO::fromEntity);

        return ResponseEntity.ok(response);
    }

    /**
     * Refresh holdings from web source for web-based importers
     * POST /api/etfs/{id}/refresh
     */
    @PostMapping("/{id}/refresh")
    public ResponseEntity<Map<String, Object>> refreshWebHoldings(@PathVariable Long id) {
        log.info("REST request to refresh web holdings for ETF ID: {}", id);

        try {
            List<String> unmappedSectors = etfService.refreshWebHoldings(id);

            Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", true);
            response.put("message", "Holdings successfully refreshed from web source");

            if (!unmappedSectors.isEmpty()) {
                response.put("warnings", List.of(
                    String.format("Unmapped sectors (could not be mapped to GICS): %s",
                        String.join(", ", unmappedSectors))
                ));
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error refreshing web holdings for ETF ID: {}", id, e);

            Map<String, Object> errorResponse = Map.of(
                "success", false,
                "message", "Failed to refresh holdings: " + e.getMessage()
            );

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get statistics for an ETF including country and sector allocations
     * GET /api/etfs/{id}/statistics
     */
    @GetMapping("/{id}/statistics")
    public ResponseEntity<ETFStatistics> getETFStatistics(@PathVariable Long id) {
        log.info("REST request to get statistics for ETF ID: {}", id);

        ETF etf = etfService.findById(id);
        List<ETFAllocation> allocations = etfService.getCurrentAllocation(id);

        // Calculate country allocations
        Map<String, CountryAccumulator> countryMap = new HashMap<>();
        for (ETFAllocation allocation : allocations) {
            Stock stock = allocation.getStock();
            String country = stock.getCountry() != null ? stock.getCountry() : "XX";
            countryMap.computeIfAbsent(country, k -> new CountryAccumulator(k))
                .addPercentage(allocation.getPercentage());
        }

        List<CountryAllocation> countryAllocations = countryMap.values().stream()
            .map(acc -> CountryAllocation.builder()
                .countryCode(acc.countryCode)
                .percentage(acc.totalPercentage.setScale(2, RoundingMode.HALF_UP))
                .stockCount(acc.stockCount)
                .build())
            .sorted((a, b) -> b.getPercentage().compareTo(a.getPercentage()))
            .collect(Collectors.toList());

        // Calculate sector allocations
        Map<String, SectorAccumulator> sectorMap = new HashMap<>();
        for (ETFAllocation allocation : allocations) {
            Stock stock = allocation.getStock();
            String sector = stock.getSector() != null && !stock.getSector().isEmpty()
                ? stock.getSector() : "Unbekannt";
            sectorMap.computeIfAbsent(sector, k -> new SectorAccumulator(k))
                .addPercentage(allocation.getPercentage());
        }

        List<SectorAllocation> sectorAllocations = sectorMap.values().stream()
            .map(acc -> SectorAllocation.builder()
                .sector(acc.sector)
                .percentage(acc.totalPercentage.setScale(2, RoundingMode.HALF_UP).doubleValue())
                .stockCount(acc.stockCount)
                .build())
            .sorted((a, b) -> Double.compare(b.getPercentage(), a.getPercentage()))
            .collect(Collectors.toList());

        // Count unique countries
        int totalCountries = (int) countryAllocations.size();

        ETFStatistics statistics = ETFStatistics.builder()
            .etfId(etf.getId())
            .etfName(etf.getName())
            .totalStocks(allocations.size())
            .totalCountries(totalCountries)
            .countryAllocations(countryAllocations)
            .sectorAllocations(sectorAllocations)
            .build();

        return ResponseEntity.ok(statistics);
    }

    // Helper classes for accumulation
    private static class CountryAccumulator {
        String countryCode;
        BigDecimal totalPercentage = BigDecimal.ZERO;
        int stockCount = 0;

        CountryAccumulator(String countryCode) {
            this.countryCode = countryCode;
        }

        void addPercentage(BigDecimal percentage) {
            this.totalPercentage = this.totalPercentage.add(percentage);
            this.stockCount++;
        }
    }

    private static class SectorAccumulator {
        String sector;
        BigDecimal totalPercentage = BigDecimal.ZERO;
        int stockCount = 0;

        SectorAccumulator(String sector) {
            this.sector = sector;
        }

        void addPercentage(BigDecimal percentage) {
            this.totalPercentage = this.totalPercentage.add(percentage);
            this.stockCount++;
        }
    }
}
