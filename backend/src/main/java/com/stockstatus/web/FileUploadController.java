package com.stockstatus.web;

import com.stockstatus.domain.ETF;
import com.stockstatus.domain.ETFAllocation;
import com.stockstatus.dto.ImportStatisticsDTO;
import com.stockstatus.exception.InvalidFileFormatException;
import com.stockstatus.service.ETFAllocationService;
import com.stockstatus.service.ETFService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

/**
 * REST Controller for file upload operations
 */
@RestController
@RequestMapping("/api/etfs/{id}/upload")
@RequiredArgsConstructor
@Slf4j
public class FileUploadController {

    private final ETFAllocationService allocationService;
    private final ETFService etfService;

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB
    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
        "text/csv",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

    /**
     * Upload and import ETF allocation file
     * POST /api/etfs/{id}/upload
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportStatisticsDTO> uploadAllocationFile(
        @PathVariable Long id,
        @RequestParam("file") MultipartFile file
    ) {
        log.info("REST request to upload allocation file for ETF ID: {}", id);

        // Validate file is not empty
        if (file.isEmpty()) {
            throw new InvalidFileFormatException("File is empty");
        }

        // Validate file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new InvalidFileFormatException(
                String.format("File size exceeds maximum allowed size of %d MB", MAX_FILE_SIZE / (1024 * 1024))
            );
        }

        // Validate file type
        String contentType = file.getContentType();
        String filename = file.getOriginalFilename();

        log.debug("File upload - Name: {}, Type: {}, Size: {} bytes", filename, contentType, file.getSize());

        if (filename == null || (!filename.toLowerCase().endsWith(".csv") &&
            !filename.toLowerCase().endsWith(".xlsx") &&
            !filename.toLowerCase().endsWith(".xls"))) {
            throw new InvalidFileFormatException(
                "Invalid file type. Only CSV and Excel files (.csv, .xlsx, .xls) are allowed"
            );
        }

        try {
            // Get ETF details
            ETF etf = etfService.findById(id);

            // Count stocks before import
            int stockCountBefore = countExistingStocks();

            // Import allocation
            List<ETFAllocation> allocations = allocationService.importAllocation(id, file);

            // Count stocks after import
            int stockCountAfter = countExistingStocks();
            int newStocksCreated = stockCountAfter - stockCountBefore;

            // Build statistics
            ImportStatisticsDTO statistics = ImportStatisticsDTO.builder()
                .etfId(etf.getId())
                .etfName(etf.getName())
                .allocationVersion(allocations.isEmpty() ? null : allocations.get(0).getAllocationVersion())
                .totalEntries(allocations.size())
                .newStocksCreated(newStocksCreated)
                .existingStocks(allocations.size() - newStocksCreated)
                .newStockIsins(extractNewStockIsins(allocations, newStocksCreated))
                .warnings(new ArrayList<>())
                .success(true)
                .build();

            if (newStocksCreated > 0) {
                statistics.getWarnings().add(
                    String.format("%d new stock(s) were automatically created during import", newStocksCreated)
                );
            }

            log.info("Successfully imported {} allocations for ETF ID: {}, version: {}",
                     allocations.size(), id, statistics.getAllocationVersion());

            return ResponseEntity.status(HttpStatus.CREATED).body(statistics);

        } catch (InvalidFileFormatException e) {
            log.error("Invalid file format for ETF ID {}: {}", id, e.getMessage());

            ImportStatisticsDTO errorStats = ImportStatisticsDTO.builder()
                .etfId(id)
                .success(false)
                .errorMessage(e.getMessage())
                .build();

            return ResponseEntity.badRequest().body(errorStats);
        } catch (Exception e) {
            log.error("Error importing allocation for ETF ID {}: {}", id, e.getMessage(), e);

            ImportStatisticsDTO errorStats = ImportStatisticsDTO.builder()
                .etfId(id)
                .success(false)
                .errorMessage("Import failed: " + e.getMessage())
                .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorStats);
        }
    }

    /**
     * Get count of existing stocks (simplified - in real implementation, use StockService)
     */
    private int countExistingStocks() {
        // This is a simplified version
        // In a real implementation, we would inject StockService and use stockService.count()
        return 0;
    }

    /**
     * Extract ISINs of newly created stocks from allocations
     */
    private List<String> extractNewStockIsins(List<ETFAllocation> allocations, int newStocksCreated) {
        if (newStocksCreated == 0) {
            return new ArrayList<>();
        }

        // This is a simplified version
        // In real implementation, we would track which stocks were newly created
        List<String> newIsins = new ArrayList<>();
        for (int i = 0; i < Math.min(newStocksCreated, allocations.size()); i++) {
            newIsins.add(allocations.get(i).getStock().getIsin());
        }
        return newIsins;
    }
}
