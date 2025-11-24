package com.stockstatus.repository;

import com.stockstatus.domain.Stock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Stock entity operations
 */
@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {

    /**
     * Find a stock by its ISIN
     * @param isin the ISIN to search for
     * @return Optional containing the stock if found
     */
    Optional<Stock> findByIsin(String isin);

    /**
     * Find a stock by its exact name (case-insensitive)
     * @param name the name to search for
     * @return Optional containing the stock if found
     */
    @Query("SELECT s FROM Stock s WHERE LOWER(s.name) = LOWER(:name)")
    Optional<Stock> findByNameIgnoreCase(@Param("name") String name);

    /**
     * Find all stocks in a specific country
     * @param country ISO 3166-1 alpha-2 country code
     * @return List of stocks in that country
     */
    List<Stock> findByCountry(String country);

    /**
     * Find all stocks in a specific country with pagination
     * @param country ISO 3166-1 alpha-2 country code
     * @param pageable pagination information
     * @return Page of stocks in that country
     */
    Page<Stock> findByCountry(String country, Pageable pageable);

    /**
     * Find all stocks in a specific sector
     * @param sector the sector to search for
     * @return List of stocks in that sector
     */
    List<Stock> findBySector(String sector);

    /**
     * Search stocks by name (case-insensitive, partial match)
     * @param name the name to search for
     * @param pageable pagination information
     * @return Page of matching stocks
     */
    @Query("SELECT s FROM Stock s WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<Stock> searchByName(@Param("name") String name, Pageable pageable);

    /**
     * Search stocks by name or ISIN (case-insensitive, partial match)
     * @param query the search query
     * @param pageable pagination information
     * @return Page of matching stocks
     */
    @Query("SELECT s FROM Stock s WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(s.isin) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Stock> searchByNameOrIsin(@Param("query") String query, Pageable pageable);

    /**
     * Check if a stock with the given ISIN already exists
     * @param isin the ISIN to check
     * @return true if a stock with this ISIN exists
     */
    boolean existsByIsin(String isin);

    /**
     * Count stocks by country
     * @param country ISO 3166-1 alpha-2 country code
     * @return number of stocks in that country
     */
    long countByCountry(String country);
}
