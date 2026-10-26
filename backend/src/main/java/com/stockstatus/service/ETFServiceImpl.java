package com.stockstatus.service;

import com.stockstatus.domain.ETF;
import com.stockstatus.domain.ETFAllocation;
import com.stockstatus.domain.ImporterType;
import com.stockstatus.domain.Stock;
import com.stockstatus.dto.AllocationEntry;
import com.stockstatus.exception.DuplicateETFException;
import com.stockstatus.exception.ETFNotFoundException;
import com.stockstatus.repository.ETFAllocationRepository;
import com.stockstatus.repository.ETFRepository;
import com.stockstatus.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of ETFService
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ETFServiceImpl implements ETFService {

    private final ETFRepository etfRepository;
    private final ETFAllocationRepository etfAllocationRepository;
    private final StockService stockService;
    private final StockRepository stockRepository;
    private final com.stockstatus.service.importer.ImporterFactory importerFactory;

    @Override
    public ETF createETF(ETF etf) {
        log.debug("Creating ETF with ISIN: {}", etf.getIsin());

        // Validate ISIN format (already done by @Pattern in entity, but double-check)
        validateIsinFormat(etf.getIsin());

        // Check for duplicates
        if (etfRepository.existsByIsin(etf.getIsin())) {
            log.warn("Attempted to create ETF with duplicate ISIN: {}", etf.getIsin());
            throw new DuplicateETFException(etf.getIsin());
        }

        ETF savedETF = etfRepository.save(etf);
        log.info("Created ETF with ID: {} and ISIN: {}", savedETF.getId(), savedETF.getIsin());
        return savedETF;
    }

    @Override
    public ETF updateETF(Long id, ETF etf) {
        log.debug("Updating ETF with ID: {}", id);

        ETF existingETF = findById(id);

        // If ISIN is being changed, check for duplicates
        if (!existingETF.getIsin().equals(etf.getIsin())) {
            validateIsinFormat(etf.getIsin());
            if (etfRepository.existsByIsin(etf.getIsin())) {
                log.warn("Attempted to update ETF {} with duplicate ISIN: {}", id, etf.getIsin());
                throw new DuplicateETFException(etf.getIsin());
            }
        }

        // Update fields
        existingETF.setName(etf.getName());
        existingETF.setIsin(etf.getIsin());
        existingETF.setImporterType(etf.getImporterType());
        existingETF.setWebUrl(etf.getWebUrl());
        existingETF.setTickerSymbol(etf.getTickerSymbol());

        ETF updatedETF = etfRepository.save(existingETF);
        log.info("Updated ETF with ID: {}", updatedETF.getId());
        return updatedETF;
    }

    @Override
    public void deleteETF(Long id) {
        log.debug("Deleting ETF with ID: {}", id);

        if (!etfRepository.existsById(id)) {
            throw new ETFNotFoundException(id);
        }

        // Cascade delete will handle allocations due to ON DELETE CASCADE in DB
        etfRepository.deleteById(id);
        log.info("Deleted ETF with ID: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public ETF findById(Long id) {
        return etfRepository.findById(id)
            .orElseThrow(() -> new ETFNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ETF> findByIdOptional(Long id) {
        return etfRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ETF> findByIsin(String isin) {
        return etfRepository.findByIsin(isin);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ETF> findAll(Pageable pageable) {
        return etfRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ETF> findAll() {
        return etfRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ETF> findByImporterType(ImporterType importerType) {
        return etfRepository.findByImporterType(importerType);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ETF> searchByNameOrIsin(String query, Pageable pageable) {
        return etfRepository.searchByNameOrIsin(query, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ETFAllocation> getCurrentAllocation(Long etfId) {
        log.debug("Getting current allocation for ETF ID: {}", etfId);

        // Verify ETF exists
        findById(etfId);

        List<ETFAllocation> allocations = etfAllocationRepository.findLatestByEtfId(etfId);
        log.debug("Found {} allocations for ETF ID: {}", allocations.size(), etfId);
        return allocations;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Integer, List<ETFAllocation>> getAllocationHistory(Long etfId) {
        log.debug("Getting allocation history for ETF ID: {}", etfId);

        // Verify ETF exists
        findById(etfId);

        List<Integer> versions = etfAllocationRepository.findAllVersionsByEtfId(etfId);

        Map<Integer, List<ETFAllocation>> history = versions.stream()
            .collect(Collectors.toMap(
                version -> version,
                version -> etfAllocationRepository.findByEtfIdAndAllocationVersion(etfId, version),
                (v1, v2) -> v1,
                LinkedHashMap::new
            ));

        log.debug("Found {} versions in allocation history for ETF ID: {}", history.size(), etfId);
        return history;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ETFAllocation> getAllocationByVersion(Long etfId, Integer version) {
        log.debug("Getting allocation version {} for ETF ID: {}", version, etfId);

        // Verify ETF exists
        findById(etfId);

        return etfAllocationRepository.findByEtfIdAndAllocationVersion(etfId, version);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Integer> getAllocationVersions(Long etfId) {
        log.debug("Getting allocation versions for ETF ID: {}", etfId);

        // Verify ETF exists
        findById(etfId);

        return etfAllocationRepository.findAllVersionsByEtfId(etfId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByIsin(String isin) {
        return etfRepository.existsByIsin(isin);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasAllocations(Long etfId) {
        return etfAllocationRepository.existsByEtfId(etfId);
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        return etfRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public long countByImporterType(ImporterType importerType) {
        return etfRepository.countByImporterType(importerType);
    }

    @Override
    public List<String> refreshWebHoldings(Long etfId) {
        log.info("Refreshing web holdings for ETF ID: {}", etfId);

        // Get the ETF
        ETF etf = findById(etfId);

        // Verify it's a web importer
        if (!etf.getImporterType().isWebImporter()) {
            throw new IllegalArgumentException(
                "ETF with ID " + etfId + " is not using a web importer. Importer type: " + etf.getImporterType()
            );
        }

        // Verify tickerSymbol is configured (only for importers that require it)
        if (etf.getImporterType().requiresTickerSymbol() &&
            (etf.getTickerSymbol() == null || etf.getTickerSymbol().isEmpty())) {
            throw new IllegalArgumentException(
                "ETF with ID " + etfId + " does not have a ticker symbol configured"
            );
        }

        // Verify webUrl is configured (only for importers that require it)
        // Amundi Web, Fidelity Web, L&G Web and Vanguard Web do not require webUrl as they use fixed API endpoints
        if (!etf.getImporterType().requiresTickerSymbol() &&
            etf.getImporterType() != ImporterType.AMUNDI_WEB &&
            etf.getImporterType() != ImporterType.FIDELITY_WEB &&
            etf.getImporterType() != ImporterType.LG_WEB &&
            etf.getImporterType() != ImporterType.VANGUARD_WEB &&
            (etf.getWebUrl() == null || etf.getWebUrl().isEmpty())) {
            throw new IllegalArgumentException(
                "ETF with ID " + etfId + " does not have a web URL configured"
            );
        }

        // Get the appropriate web importer
        com.stockstatus.service.importer.WebImporter webImporter = importerFactory.getWebImporter(etf.getImporterType());

        // Fetch and parse holdings from web
        List<AllocationEntry> allocationEntries;

        // Handle different web importer types
        if (webImporter instanceof com.stockstatus.service.importer.VanEckWebImporter) {
            // VanEck Web requires only ticker symbol
            log.debug("Fetching holdings for ticker symbol: {}", etf.getTickerSymbol());
            allocationEntries = webImporter.fetchAndParse(etf.getTickerSymbol());
        } else if (webImporter instanceof com.stockstatus.service.importer.AmundiWebImporter) {
            // Amundi Web requires only ISIN (uses fixed API endpoint)
            log.debug("Fetching holdings for ISIN: {}", etf.getIsin());
            allocationEntries = webImporter.fetchAndParse(etf.getIsin());
        } else if (webImporter instanceof com.stockstatus.service.importer.FidelityWebImporter) {
            // Fidelity Web requires only ISIN (uses fixed API endpoint)
            log.debug("Fetching holdings for ISIN: {}", etf.getIsin());
            allocationEntries = webImporter.fetchAndParse(etf.getIsin());
        } else if (webImporter instanceof com.stockstatus.service.importer.LGWebImporter) {
            // L&G Web requires only ISIN (resolves the holdings document from the fund centre API)
            log.debug("Fetching holdings for ISIN: {}", etf.getIsin());
            allocationEntries = webImporter.fetchAndParse(etf.getIsin());
        } else if (webImporter instanceof com.stockstatus.service.importer.VanguardWebImporter) {
            // Vanguard Web requires only ISIN (resolves the fund's portId via the GraphQL API)
            log.debug("Fetching holdings for ISIN: {}", etf.getIsin());
            allocationEntries = webImporter.fetchAndParse(etf.getIsin());
        } else {
            // iShares Web, XTrackers Web and others require only webUrl
            log.debug("Fetching holdings from URL: {}", etf.getWebUrl());
            allocationEntries = webImporter.fetchAndParse(etf.getWebUrl());
        }

        log.info("Fetched {} allocation entries from web source", allocationEntries.size());

        // Collect unique unmapped sectors
        Set<String> unmappedSectors = allocationEntries.stream()
            .map(AllocationEntry::getOriginalSector)
            .filter(originalSector -> originalSector != null && !originalSector.isEmpty())
            .collect(Collectors.toCollection(LinkedHashSet::new));

        // Determine the next allocation version
        List<Integer> existingVersions = etfAllocationRepository.findAllVersionsByEtfId(etfId);
        int nextVersion = existingVersions.isEmpty() ? 1 : existingVersions.get(0) + 1;
        log.debug("Creating new allocation version: {}", nextVersion);

        // Resolve stocks against an in-memory index instead of querying per entry: with thousands
        // of holdings (e.g. Vanguard FTSE All-Cap, ~7000), each query also auto-flushes the growing
        // persistence context, which made the refresh take minutes.
        StockIndex stockIndex = new StockIndex(stockService.findAll());

        // Process each allocation entry
        List<ETFAllocation> allocations = new ArrayList<>();

        for (AllocationEntry entry : allocationEntries) {
            // Find or create stock
            Stock stock = findOrCreateStock(entry, stockIndex);

            // Create ETF allocation
            ETFAllocation allocation = ETFAllocation.builder()
                .etf(etf)
                .stock(stock)
                .percentage(entry.getPercentage())
                .allocationVersion(nextVersion)
                .build();

            allocations.add(allocation);
        }

        // Save new stocks first, then all allocations
        if (!stockIndex.newStocks.isEmpty()) {
            stockRepository.saveAll(stockIndex.newStocks);
            log.info("Created {} new stocks for ETF ID: {}", stockIndex.newStocks.size(), etfId);
        }
        etfAllocationRepository.saveAll(allocations);

        log.info("Successfully saved {} allocations as version {} for ETF ID: {}",
            allocations.size(), nextVersion, etfId);

        return new ArrayList<>(unmappedSectors);
    }

    /**
     * In-memory lookup of all stocks by ISIN and case-insensitive name. Stocks created during the
     * refresh are added to it as well, so that duplicates within one import resolve to the same
     * stock; they are persisted in one go via {@link #newStocks}.
     */
    private static final class StockIndex {
        private final Map<String, Stock> byIsin = new HashMap<>();
        private final Map<String, Stock> byName = new HashMap<>();
        private final List<Stock> newStocks = new ArrayList<>();

        StockIndex(List<Stock> stocks) {
            stocks.forEach(this::add);
        }

        Optional<Stock> findByIsin(String isin) {
            return Optional.ofNullable(byIsin.get(isin));
        }

        Optional<Stock> findByName(String name) {
            return name == null ? Optional.empty() : Optional.ofNullable(byName.get(name.toLowerCase()));
        }

        void addNew(Stock stock) {
            add(stock);
            newStocks.add(stock);
        }

        private void add(Stock stock) {
            byIsin.put(stock.getIsin(), stock);
            if (stock.getName() != null) {
                byName.putIfAbsent(stock.getName().toLowerCase(), stock);
            }
        }
    }

    /**
     * Find an existing stock by ISIN or create a new one
     * @param entry the allocation entry containing stock information
     * @param stockIndex lookup of all known stocks
     * @return the found or created stock
     */
    private Stock findOrCreateStock(AllocationEntry entry, StockIndex stockIndex) {
        // Check if ISIN is valid (not null, not empty, not just a dash or placeholder)
        boolean hasValidIsin = entry.getIsin() != null
            && !entry.getIsin().isEmpty()
            && !entry.getIsin().equals("-")
            && entry.getIsin().matches("^[A-Z]{2}[A-Z0-9]{9}[0-9]$");

        // First try to find by ISIN if it's valid
        if (hasValidIsin) {
            Optional<Stock> existingStock = stockIndex.findByIsin(entry.getIsin());
            if (existingStock.isPresent()) {
                Stock stock = existingStock.get();
                log.debug("Found existing stock by ISIN: {}", entry.getIsin());

                // Update sector if current sector is "Unbekannt" and we have a better sector from import
                updateSectorIfUnknown(stock, entry.getSector());

                return stock;
            }
        }

        // If not found by ISIN, try to find by name
        Optional<Stock> existingStockByName = stockIndex.findByName(entry.getName());
        if (existingStockByName.isPresent()) {
            Stock stock = existingStockByName.get();
            log.debug("Found existing stock by name: {}", entry.getName());

            // Update sector if current sector is "Unbekannt" and we have a better sector from import
            updateSectorIfUnknown(stock, entry.getSector());

            return stock;
        }

        // Create new stock
        String isinToUse = hasValidIsin ? entry.getIsin() : generatePlaceholderIsin(stockIndex);
        log.debug("Creating new stock: {} (ISIN: {})", entry.getName(), isinToUse);

        Stock newStock = Stock.builder()
            .name(entry.getName())
            .isin(isinToUse)
            .country(entry.getCountry() != null ? entry.getCountry() : "XX")
            .sector(entry.getSector())
            .build();

        stockIndex.addNew(newStock);
        return newStock;
    }

    /**
     * Update the stock information from import data if the import has better data
     * @param stock The stock to potentially update
     * @param newSector The new sector value from the import
     */
    private void updateSectorIfUnknown(Stock stock, String newSector) {
        // Update sector if import has a valid sector and current is "Unbekannt", null, or empty.
        // The stock is a managed entity (or a new one saved later), so no explicit save is needed.
        if (newSector != null &&
            !newSector.isEmpty() &&
            !"Unbekannt".equals(newSector) &&
            (stock.getSector() == null || stock.getSector().isEmpty() || "Unbekannt".equals(stock.getSector()))) {

            log.info("Updating sector for stock '{}' (ISIN: {}) from '{}' to '{}'",
                stock.getName(), stock.getIsin(), stock.getSector(), newSector);

            stock.setSector(newSector);
        }
    }

    /**
     * Generate a placeholder ISIN for stocks without a valid ISIN
     * Uses the SONSTIGE prefix with a random number
     */
    private String generatePlaceholderIsin(StockIndex stockIndex) {
        // Generate a random 5-digit number that is not taken yet
        String isin;
        do {
            int randomNumber = (int) (Math.random() * 100000);
            isin = String.format("SONSTIGE%05d", randomNumber);
        } while (stockIndex.findByIsin(isin).isPresent());
        return isin;
    }

    @Override
    public ETF updateLogo(Long id, byte[] logo, String contentType) {
        log.debug("Updating logo for ETF with ID: {}", id);

        ETF existingEtf = findById(id);
        existingEtf.setLogo(logo);
        existingEtf.setLogoContentType(contentType);

        ETF updatedEtf = etfRepository.save(existingEtf);
        log.info("Updated logo for ETF with ID: {}, size: {} bytes", id, logo.length);
        return updatedEtf;
    }

    /**
     * Validate ISIN format
     * @param isin the ISIN to validate
     * @throws IllegalArgumentException if ISIN format is invalid
     */
    private void validateIsinFormat(String isin) {
        if (isin == null || !isin.matches("^[A-Z]{2}[A-Z0-9]{9}[0-9]$")) {
            throw new IllegalArgumentException("Invalid ISIN format: " + isin);
        }
    }
}
