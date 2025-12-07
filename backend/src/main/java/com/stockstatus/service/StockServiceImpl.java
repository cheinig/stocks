package com.stockstatus.service;

import com.stockstatus.domain.Stock;
import com.stockstatus.exception.DuplicateStockException;
import com.stockstatus.exception.StockNotFoundException;
import com.stockstatus.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Implementation of StockService
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class StockServiceImpl implements StockService {

    private final StockRepository stockRepository;

    @Override
    public Stock createStock(Stock stock) {
        log.debug("Creating stock with ISIN: {}", stock.getIsin());

        // Validate ISIN format (already done by @Pattern in entity, but double-check)
        validateIsinFormat(stock.getIsin());

        // Check for duplicates
        if (stockRepository.existsByIsin(stock.getIsin())) {
            log.warn("Attempted to create stock with duplicate ISIN: {}", stock.getIsin());
            throw new DuplicateStockException(stock.getIsin());
        }

        Stock savedStock = stockRepository.save(stock);
        log.info("Created stock with ID: {} and ISIN: {}", savedStock.getId(), savedStock.getIsin());
        return savedStock;
    }

    @Override
    public Stock updateStock(Long id, Stock stock) {
        log.debug("Updating stock with ID: {}", id);

        Stock existingStock = findById(id);

        // If ISIN is being changed, check for duplicates
        if (!existingStock.getIsin().equals(stock.getIsin())) {
            validateIsinFormat(stock.getIsin());
            if (stockRepository.existsByIsin(stock.getIsin())) {
                log.warn("Attempted to update stock {} with duplicate ISIN: {}", id, stock.getIsin());
                throw new DuplicateStockException(stock.getIsin());
            }
        }

        // Update fields
        existingStock.setName(stock.getName());
        existingStock.setIsin(stock.getIsin());
        existingStock.setCountry(stock.getCountry());
        existingStock.setSector(stock.getSector());

        Stock updatedStock = stockRepository.save(existingStock);
        log.info("Updated stock with ID: {}", updatedStock.getId());
        return updatedStock;
    }

    @Override
    public void deleteStock(Long id) {
        log.debug("Deleting stock with ID: {}", id);

        if (!stockRepository.existsById(id)) {
            throw new StockNotFoundException(id);
        }

        stockRepository.deleteById(id);
        log.info("Deleted stock with ID: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public Stock findById(Long id) {
        return stockRepository.findById(id)
            .orElseThrow(() -> new StockNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Stock> findByIdOptional(Long id) {
        return stockRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Stock> findByIsin(String isin) {
        return stockRepository.findByIsin(isin);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Stock> findByName(String name) {
        return stockRepository.findByNameIgnoreCase(name);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Stock> findAll(Pageable pageable) {
        return stockRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Stock> findAll() {
        return stockRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Stock> findByCountry(String country, Pageable pageable) {
        return stockRepository.findByCountry(country, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Stock> findBySector(String sector) {
        return stockRepository.findBySector(sector);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Stock> searchByNameOrIsin(String query, Pageable pageable) {
        return stockRepository.searchByNameOrIsin(query, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByIsin(String isin) {
        return stockRepository.existsByIsin(isin);
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        return stockRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public long countByCountry(String country) {
        return stockRepository.countByCountry(country);
    }

    @Override
    public Stock updateLogo(Long id, byte[] logo, String contentType) {
        log.debug("Updating logo for stock with ID: {}", id);

        Stock existingStock = findById(id);
        existingStock.setLogo(logo);
        existingStock.setLogoContentType(contentType);

        Stock updatedStock = stockRepository.save(existingStock);
        log.info("Updated logo for stock with ID: {}, size: {} bytes", id, logo.length);
        return updatedStock;
    }

    /**
     * Validate ISIN format
     * @param isin the ISIN to validate
     * @throws IllegalArgumentException if ISIN format is invalid
     */
    private void validateIsinFormat(String isin) {
        if (isin == null || !isin.matches("^([A-Z]{2}[A-Z0-9]{9}[0-9]|SONSTIGE[0-9]{5})$")) {
            throw new IllegalArgumentException("Invalid ISIN format: " + isin);
        }
    }
}
