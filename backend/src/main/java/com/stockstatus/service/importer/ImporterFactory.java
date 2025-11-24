package com.stockstatus.service.importer;

import com.stockstatus.domain.ImporterType;
import com.stockstatus.exception.InvalidFileFormatException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Factory service to select the appropriate file importer based on ImporterType
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ImporterFactory {

    private final GenericCSVImporter genericCSVImporter;
    private final GenericExcelImporter genericExcelImporter;
    private final FidelityExcelImporter fidelityExcelImporter;

    /**
     * Get the appropriate importer for the given ImporterType
     * @param importerType the type of importer to use
     * @return the file importer
     * @throws IllegalArgumentException if the importer type is not supported
     */
    public FileImporter getImporter(ImporterType importerType) {
        log.debug("Getting importer for type: {}", importerType);

        switch (importerType) {
            case GENERIC_CSV:
            case ISHARES_CSV:
            case VANGUARD_CSV:
            case SPDR_CSV:
                return genericCSVImporter;

            case GENERIC_EXCEL:
                return genericExcelImporter;

            case FIDELITY:
                return fidelityExcelImporter;

            default:
                throw new IllegalArgumentException("Unsupported importer type: " + importerType);
        }
    }

    /**
     * Get the appropriate importer for the given file based on file extension
     * @param file the file to import
     * @return the file importer
     * @throws InvalidFileFormatException if no suitable importer is found
     */
    public FileImporter getImporterForFile(MultipartFile file) {
        log.debug("Getting importer for file: {}", file.getOriginalFilename());

        // Check specific importers first (like Fidelity), then fall back to generic ones
        List<FileImporter> importers = List.of(fidelityExcelImporter, genericCSVImporter, genericExcelImporter);

        for (FileImporter importer : importers) {
            if (importer.supports(file)) {
                log.debug("Selected importer: {}", importer.getImporterName());
                return importer;
            }
        }

        throw new InvalidFileFormatException(
            "Unknown",
            "No suitable importer found for file: " + file.getOriginalFilename()
        );
    }

    /**
     * Validate that the file is compatible with the specified ImporterType
     * @param file the file to validate
     * @param importerType the expected importer type
     * @throws InvalidFileFormatException if the file is not compatible
     */
    public void validateFileForImporterType(MultipartFile file, ImporterType importerType) {
        FileImporter importer = getImporter(importerType);

        if (!importer.supports(file)) {
            throw new InvalidFileFormatException(
                importerType.name(),
                "File format is not compatible with " + importerType.getDisplayName()
            );
        }
    }
}
