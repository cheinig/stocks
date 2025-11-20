package com.stockstatus.repository;

import com.stockstatus.domain.ETF;
import com.stockstatus.domain.ETFAllocation;
import com.stockstatus.domain.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for ETFAllocation entity operations
 */
@Repository
public interface ETFAllocationRepository extends JpaRepository<ETFAllocation, Long> {

    /**
     * Find all allocations for a specific ETF
     * @param etf the ETF to find allocations for
     * @return List of allocations for that ETF
     */
    List<ETFAllocation> findByEtf(ETF etf);

    /**
     * Find all allocations for a specific ETF by ID
     * @param etfId the ETF ID to find allocations for
     * @return List of allocations for that ETF
     */
    List<ETFAllocation> findByEtfId(Long etfId);

    /**
     * Find all allocations for a specific ETF and version
     * @param etfId the ETF ID
     * @param allocationVersion the allocation version
     * @return List of allocations for that ETF and version
     */
    List<ETFAllocation> findByEtfIdAndAllocationVersion(Long etfId, Integer allocationVersion);

    /**
     * Find the latest allocation version for a specific ETF
     * @param etfId the ETF ID
     * @return List of allocations for the latest version
     */
    @Query("SELECT a FROM ETFAllocation a WHERE a.etf.id = :etfId AND a.allocationVersion = " +
           "(SELECT MAX(a2.allocationVersion) FROM ETFAllocation a2 WHERE a2.etf.id = :etfId)")
    List<ETFAllocation> findLatestByEtfId(@Param("etfId") Long etfId);

    /**
     * Find all allocations that reference a specific stock
     * @param stock the stock to find allocations for
     * @return List of allocations that include this stock
     */
    List<ETFAllocation> findByStock(Stock stock);

    /**
     * Find all allocations that reference a specific stock by ID
     * @param stockId the stock ID to find allocations for
     * @return List of allocations that include this stock
     */
    List<ETFAllocation> findByStockId(Long stockId);

    /**
     * Get the maximum allocation version for a specific ETF
     * @param etfId the ETF ID
     * @return the maximum version number, or null if no allocations exist
     */
    @Query("SELECT MAX(a.allocationVersion) FROM ETFAllocation a WHERE a.etf.id = :etfId")
    Integer findMaxVersionByEtfId(@Param("etfId") Long etfId);

    /**
     * Get all distinct allocation versions for a specific ETF, ordered descending
     * @param etfId the ETF ID
     * @return List of version numbers
     */
    @Query("SELECT DISTINCT a.allocationVersion FROM ETFAllocation a WHERE a.etf.id = :etfId ORDER BY a.allocationVersion DESC")
    List<Integer> findAllVersionsByEtfId(@Param("etfId") Long etfId);

    /**
     * Delete all allocations for a specific ETF and version
     * @param etfId the ETF ID
     * @param allocationVersion the version to delete
     */
    @Modifying
    @Query("DELETE FROM ETFAllocation a WHERE a.etf.id = :etfId AND a.allocationVersion = :version")
    void deleteByEtfIdAndAllocationVersion(@Param("etfId") Long etfId, @Param("version") Integer allocationVersion);

    /**
     * Delete all allocations for a specific ETF
     * @param etfId the ETF ID
     */
    @Modifying
    @Query("DELETE FROM ETFAllocation a WHERE a.etf.id = :etfId")
    void deleteByEtfId(@Param("etfId") Long etfId);

    /**
     * Count allocations for a specific ETF and version
     * @param etfId the ETF ID
     * @param allocationVersion the allocation version
     * @return number of allocations
     */
    long countByEtfIdAndAllocationVersion(Long etfId, Integer allocationVersion);

    /**
     * Check if allocations exist for a specific ETF
     * @param etfId the ETF ID
     * @return true if allocations exist
     */
    boolean existsByEtfId(Long etfId);
}
