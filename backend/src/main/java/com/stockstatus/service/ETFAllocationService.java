package com.stockstatus.service;

import com.stockstatus.domain.ETFAllocation;
import com.stockstatus.dto.AllocationEntry;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Service for managing ETF allocations
 */
public interface ETFAllocationService {

    /**
     * Import and save a new allocation version for an ETF from a file
     * @param etfId the ETF ID
     * @param file the uploaded file containing allocation data
     * @return List of created allocations
     * @throws com.stockstatus.exception.ETFNotFoundException if ETF not found
     * @throws com.stockstatus.exception.InvalidFileFormatException if file format is invalid
     * @throws com.stockstatus.exception.AllocationSumException if allocation sum is invalid
     */
    List<ETFAllocation> importAllocation(Long etfId, MultipartFile file);

    /**
     * Import and save a new allocation version for an ETF from parsed allocation entries
     * @param etfId the ETF ID
     * @param entries the parsed allocation entries
     * @return List of created allocations
     * @throws com.stockstatus.exception.ETFNotFoundException if ETF not found
     */
    List<ETFAllocation> saveAllocation(Long etfId, List<AllocationEntry> entries);

    /**
     * Get the current (latest) allocation for an ETF
     * @param etfId the ETF ID
     * @return List of allocations for the latest version
     */
    List<ETFAllocation> getCurrentAllocation(Long etfId);

    /**
     * Get a specific allocation version for an ETF
     * @param etfId the ETF ID
     * @param version the version number
     * @return List of allocations for that version
     */
    List<ETFAllocation> getAllocationByVersion(Long etfId, Integer version);

    /**
     * Delete a specific allocation version
     * @param etfId the ETF ID
     * @param version the version to delete
     */
    void deleteAllocationVersion(Long etfId, Integer version);

    /**
     * Delete old allocation versions, keeping only the specified number of most recent versions
     * @param etfId the ETF ID
     * @param versionsToKeep number of most recent versions to keep
     * @return number of deleted versions
     */
    int deleteOldVersions(Long etfId, int versionsToKeep);

    /**
     * Get all allocation versions for an ETF
     * @param etfId the ETF ID
     * @return List of version numbers, ordered descending
     */
    List<Integer> getAllVersions(Long etfId);

    /**
     * Get the next version number for an ETF
     * @param etfId the ETF ID
     * @return the next version number
     */
    Integer getNextVersion(Long etfId);
}
