package com.stockstatus.service;

import com.stockstatus.domain.Stock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for Stock operations
 */
public interface StockService {

    /**
     * Create a new stock
     * @param stock the stock to create
     * @return the created stock
     * @throws com.stockstatus.exception.DuplicateStockException if a stock with the same ISIN already exists
     */
    Stock createStock(Stock stock);

    /**
     * Update an existing stock
     * @param id the ID of the stock to update
     * @param stock the updated stock data
     * @return the updated stock
     * @throws com.stockstatus.exception.StockNotFoundException if the stock is not found
     * @throws com.stockstatus.exception.DuplicateStockException if another stock with the same ISIN already exists
     */
    Stock updateStock(Long id, Stock stock);

    /**
     * Delete a stock by ID
     * @param id the ID of the stock to delete
     * @throws com.stockstatus.exception.StockNotFoundException if the stock is not found
     */
    void deleteStock(Long id);

    /**
     * Find a stock by ID
     * @param id the ID to search for
     * @return the stock
     * @throws com.stockstatus.exception.StockNotFoundException if the stock is not found
     */
    Stock findById(Long id);

    /**
     * Find a stock by ID, returning Optional
     * @param id the ID to search for
     * @return Optional containing the stock if found
     */
    Optional<Stock> findByIdOptional(Long id);

    /**
     * Find a stock by ISIN
     * @param isin the ISIN to search for
     * @return Optional containing the stock if found
     */
    Optional<Stock> findByIsin(String isin);

    /**
     * Find a stock by exact name (case-insensitive)
     * @param name the name to search for
     * @return Optional containing the stock if found
     */
    Optional<Stock> findByName(String name);

    /**
     * Find all stocks with pagination
     * @param pageable pagination information
     * @return Page of stocks
     */
    Page<Stock> findAll(Pageable pageable);

    /**
     * Find all stocks
     * @return List of all stocks
     */
    List<Stock> findAll();

    /**
     * Find stocks by country
     * @param country ISO 3166-1 alpha-2 country code
     * @param pageable pagination information
     * @return Page of stocks in that country
     */
    Page<Stock> findByCountry(String country, Pageable pageable);

    /**
     * Find stocks by sector
     * @param sector the sector to search for
     * @return List of stocks in that sector
     */
    List<Stock> findBySector(String sector);

    /**
     * Search stocks by name or ISIN
     * @param query the search query
     * @param pageable pagination information
     * @return Page of matching stocks
     */
    Page<Stock> searchByNameOrIsin(String query, Pageable pageable);

    /**
     * Check if a stock with the given ISIN exists
     * @param isin the ISIN to check
     * @return true if a stock exists with this ISIN
     */
    boolean existsByIsin(String isin);

    /**
     * Count all stocks
     * @return total number of stocks
     */
    long count();

    /**
     * Count stocks by country
     * @param country ISO 3166-1 alpha-2 country code
     * @return number of stocks in that country
     */
    long countByCountry(String country);

    /**
     * Update logo for a stock
     * @param id the ID of the stock
     * @param logo the logo bytes
     * @param contentType the content type of the logo
     * @return the updated stock
     */
    Stock updateLogo(Long id, byte[] logo, String contentType);
}
