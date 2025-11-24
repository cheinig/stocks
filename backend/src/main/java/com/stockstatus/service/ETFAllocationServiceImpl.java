package com.stockstatus.service;

import com.stockstatus.domain.ETF;
import com.stockstatus.domain.ETFAllocation;
import com.stockstatus.domain.ImporterType;
import com.stockstatus.domain.Stock;
import com.stockstatus.dto.AllocationEntry;
import com.stockstatus.repository.ETFAllocationRepository;
import com.stockstatus.service.importer.FileImporter;
import com.stockstatus.service.importer.ImporterFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of ETFAllocationService
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ETFAllocationServiceImpl implements ETFAllocationService {

    private final ETFAllocationRepository allocationRepository;
    private final ETFService etfService;
    private final StockService stockService;
    private final ImporterFactory importerFactory;

    @Override
    public List<ETFAllocation> importAllocation(Long etfId, MultipartFile file) {
        log.info("Importing allocation from file for ETF ID: {}", etfId);

        // Verify ETF exists
        ETF etf = etfService.findById(etfId);

        // Get appropriate importer
        ImporterType importerType = etf.getImporterType();
        FileImporter importer = importerFactory.getImporter(importerType);

        // Validate file format
        importerFactory.validateFileForImporterType(file, importerType);

        // Parse file
        List<AllocationEntry> entries = importer.parseFile(file);

        // Save allocation
        return saveAllocation(etfId, entries);
    }

    @Override
    public List<ETFAllocation> saveAllocation(Long etfId, List<AllocationEntry> entries) {
        log.info("Saving {} allocation entries for ETF ID: {}", entries.size(), etfId);

        // Verify ETF exists
        ETF etf = etfService.findById(etfId);

        // Get next version number
        Integer nextVersion = getNextVersion(etfId);

        List<ETFAllocation> allocations = new ArrayList<>();

        for (AllocationEntry entry : entries) {
            // Find or create stock
            Stock stock = findOrCreateStock(entry);

            // Create allocation
            ETFAllocation allocation = ETFAllocation.builder()
                .etf(etf)
                .stock(stock)
                .percentage(entry.getPercentage())
                .allocationVersion(nextVersion)
                .build();

            allocations.add(allocation);
        }

        // Save all allocations
        List<ETFAllocation> savedAllocations = allocationRepository.saveAll(allocations);

        log.info("Saved allocation version {} with {} entries for ETF ID: {}",
                 nextVersion, savedAllocations.size(), etfId);

        return savedAllocations;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ETFAllocation> getCurrentAllocation(Long etfId) {
        // Delegate to ETFService
        return etfService.getCurrentAllocation(etfId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ETFAllocation> getAllocationByVersion(Long etfId, Integer version) {
        // Verify ETF exists
        etfService.findById(etfId);
        return allocationRepository.findByEtfIdAndAllocationVersion(etfId, version);
    }

    @Override
    public void deleteAllocationVersion(Long etfId, Integer version) {
        log.info("Deleting allocation version {} for ETF ID: {}", version, etfId);

        // Verify ETF exists
        etfService.findById(etfId);

        allocationRepository.deleteByEtfIdAndAllocationVersion(etfId, version);

        log.info("Deleted allocation version {} for ETF ID: {}", version, etfId);
    }

    @Override
    public int deleteOldVersions(Long etfId, int versionsToKeep) {
        log.info("Deleting old allocation versions for ETF ID: {}, keeping {} versions",
                 etfId, versionsToKeep);

        // Verify ETF exists
        etfService.findById(etfId);

        List<Integer> allVersions = allocationRepository.findAllVersionsByEtfId(etfId);

        if (allVersions.size() <= versionsToKeep) {
            log.debug("No versions to delete, current count: {}", allVersions.size());
            return 0;
        }

        // Delete older versions (keep the most recent ones)
        int deleteCount = 0;
        for (int i = versionsToKeep; i < allVersions.size(); i++) {
            Integer versionToDelete = allVersions.get(i);
            allocationRepository.deleteByEtfIdAndAllocationVersion(etfId, versionToDelete);
            deleteCount++;
        }

        log.info("Deleted {} old allocation versions for ETF ID: {}", deleteCount, etfId);
        return deleteCount;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Integer> getAllVersions(Long etfId) {
        // Verify ETF exists
        etfService.findById(etfId);
        return allocationRepository.findAllVersionsByEtfId(etfId);
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getNextVersion(Long etfId) {
        Integer maxVersion = allocationRepository.findMaxVersionByEtfId(etfId);
        return maxVersion == null ? 1 : maxVersion + 1;
    }

    /**
     * Find existing stock by ISIN or name, or create new one
     * @param entry the allocation entry containing stock information
     * @return the found or created stock
     */
    private Stock findOrCreateStock(AllocationEntry entry) {
        // First try to find by ISIN if available
        if (entry.getIsin() != null && !entry.getIsin().isEmpty()) {
            Optional<Stock> existingStock = stockService.findByIsin(entry.getIsin());
            if (existingStock.isPresent()) {
                log.debug("Found existing stock by ISIN: {}", entry.getIsin());
                return existingStock.get();
            }
        }

        // If no ISIN or not found by ISIN, try to find by name
        Optional<Stock> stockByName = stockService.findByName(entry.getName());
        if (stockByName.isPresent()) {
            log.debug("Found existing stock by name: {}", entry.getName());
            return stockByName.get();
        }

        // Create new stock
        log.info("Stock not found, creating new stock: {} (ISIN: {})",
                 entry.getName(), entry.getIsin() != null ? entry.getIsin() : "N/A");

        Stock newStock = Stock.builder()
            .isin(entry.getIsin())
            .name(entry.getName())
            .country(entry.getCountry() != null ? entry.getCountry() : "XX") // XX = Unknown
            .sector(entry.getSector())
            .build();

        return stockService.createStock(newStock);
    }
}
