package com.stockstatus.repository;

import com.stockstatus.domain.ETF;
import com.stockstatus.domain.ImporterType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for ETF entity operations
 */
@Repository
public interface ETFRepository extends JpaRepository<ETF, Long> {

    /**
     * Find an ETF by its ISIN
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
     * Find all ETFs by importer type
     * @param importerType the importer type to filter by
     * @return List of ETFs using that importer type
     */
    List<ETF> findByImporterType(ImporterType importerType);

    /**
     * Search ETFs by name (case-insensitive, partial match)
     * @param name the name to search for
     * @param pageable pagination information
     * @return Page of matching ETFs
     */
    @Query("SELECT e FROM ETF e WHERE LOWER(e.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<ETF> searchByName(@Param("name") String name, Pageable pageable);

    /**
     * Search ETFs by name or ISIN (case-insensitive, partial match)
     * @param query the search query
     * @param pageable pagination information
     * @return Page of matching ETFs
     */
    @Query("SELECT e FROM ETF e WHERE LOWER(e.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(e.isin) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<ETF> searchByNameOrIsin(@Param("query") String query, Pageable pageable);

    /**
     * Check if an ETF with the given ISIN already exists
     * @param isin the ISIN to check
     * @return true if an ETF with this ISIN exists
     */
    boolean existsByIsin(String isin);

    /**
     * Count ETFs by importer type
     * @param importerType the importer type to count
     * @return number of ETFs using that importer type
     */
    long countByImporterType(ImporterType importerType);
}
