package com.stockstatus.service.importer;

import com.stockstatus.dto.AllocationEntry;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Interface for file importers that parse ETF allocation data from uploaded files
 */
public interface FileImporter {

    /**
     * Parse an uploaded file and extract allocation entries
     * @param file the uploaded file (CSV, Excel, etc.)
     * @return List of allocation entries parsed from the file
     * @throws com.stockstatus.exception.InvalidFileFormatException if the file format is invalid
     * @throws com.stockstatus.exception.AllocationSumException if allocation percentages don't sum to ~100%
     */
    List<AllocationEntry> parseFile(MultipartFile file);

    /**
     * Check if this importer supports the given file
     * @param file the file to check
     * @return true if this importer can handle the file
     */
    boolean supports(MultipartFile file);

    /**
     * Get the name of this importer
     * @return importer name
     */
    String getImporterName();
}
