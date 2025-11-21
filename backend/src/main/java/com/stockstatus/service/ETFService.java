package com.stockstatus.service;

import com.stockstatus.domain.ETF;
import com.stockstatus.domain.ETFAllocation;
import com.stockstatus.domain.ImporterType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service interface for ETF operations
 */
public interface ETFService {

    /**
     * Create a new ETF
     * @param etf the ETF to create
     * @return the created ETF
     * @throws com.stockstatus.exception.DuplicateETFException if an ETF with the same ISIN already exists
     */
    ETF createETF(ETF etf);

    /**
     * Update an existing ETF
     * @param id the ID of the ETF to update
     * @param etf the updated ETF data
     * @return the updated ETF
     * @throws com.stockstatus.exception.ETFNotFoundException if the ETF is not found
     * @throws com.stockstatus.exception.DuplicateETFException if another ETF with the same ISIN already exists
     */
    ETF updateETF(Long id, ETF etf);

    /**
     * Delete an ETF by ID
     * @param id the ID of the ETF to delete
     * @throws com.stockstatus.exception.ETFNotFoundException if the ETF is not found
     */
    void deleteETF(Long id);

    /**
     * Find an ETF by ID
     * @param id the ID to search for
     * @return the ETF
     * @throws com.stockstatus.exception.ETFNotFoundException if the ETF is not found
     */
    ETF findById(Long id);

    /**
     * Find an ETF by ID, returning Optional
     * @param id the ID to search for
     * @return Optional containing the ETF if found
     */
    Optional<ETF> findByIdOptional(Long id);

    /**
     * Find an ETF by ISIN
     * @param isin the ISIN to search for
     * @return Optional containing the ETF if found
     */
    Optional<ETF> findByIsin(String isin);

    /**
     * Find all ETFs with pagination
     * @param pageable pagination information
     * @return Page of ETFs
     */
    Page<ETF> findAll(Pageable pageable);

    /**
     * Find all ETFs
     * @return List of all ETFs
     */
    List<ETF> findAll();

    /**
     * Find ETFs by importer type
     * @param importerType the importer type to filter by
     * @return List of ETFs using that importer type
     */
    List<ETF> findByImporterType(ImporterType importerType);

    /**
     * Search ETFs by name or ISIN
     * @param query the search query
     * @param pageable pagination information
     * @return Page of matching ETFs
     */
    Page<ETF> searchByNameOrIsin(String query, Pageable pageable);

    /**
     * Get the current (latest) allocation for an ETF
     * @param etfId the ETF ID
     * @return List of allocations for the latest version
     */
    List<ETFAllocation> getCurrentAllocation(Long etfId);

    /**
     * Get all allocation versions for an ETF
     * @param etfId the ETF ID
     * @return Map of version number to list of allocations
     */
    Map<Integer, List<ETFAllocation>> getAllocationHistory(Long etfId);

    /**
     * Get a specific allocation version for an ETF
     * @param etfId the ETF ID
     * @param version the allocation version number
     * @return List of allocations for that version
     */
    List<ETFAllocation> getAllocationByVersion(Long etfId, Integer version);

    /**
     * Get all available allocation versions for an ETF
     * @param etfId the ETF ID
     * @return List of version numbers, ordered descending
     */
    List<Integer> getAllocationVersions(Long etfId);

    /**
     * Check if an ETF with the given ISIN exists
     * @param isin the ISIN to check
     * @return true if an ETF exists with this ISIN
     */
    boolean existsByIsin(String isin);

    /**
     * Check if an ETF has any allocations
     * @param etfId the ETF ID
     * @return true if the ETF has allocations
     */
    boolean hasAllocations(Long etfId);

    /**
     * Count all ETFs
     * @return total number of ETFs
     */
    long count();

    /**
     * Count ETFs by importer type
     * @param importerType the importer type to count
     * @return number of ETFs using that importer type
     */
    long countByImporterType(ImporterType importerType);
}
