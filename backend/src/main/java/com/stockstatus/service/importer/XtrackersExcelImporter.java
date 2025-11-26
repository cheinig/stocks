package com.stockstatus.service.importer;

import com.stockstatus.dto.AllocationEntry;
import com.stockstatus.exception.AllocationSumException;
import com.stockstatus.exception.InvalidFileFormatException;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Xtrackers Excel importer for ETF holdings
 * Expected format:
 * - Row 1: Disclaimer text (contains "DWS")
 * - Row 2: Empty
 * - Row 3: Empty
 * - Row 4: Headers (Name | ISIN | Country | Currency | Exchange | Type of Security | Rating | Primary Listing | Industry Classification | Weighting)
 * - Row 5+: Data rows
 *
 * Special handling:
 * - Country code is extracted from ISIN (first 2 characters)
 * - Sector is mapped from "Industry Classification" column
 * - Entries without valid ISIN (empty or starting with "_") are grouped as "Sonstige"
 */
@Component
@Slf4j
public class XtrackersExcelImporter implements FileImporter {

    private static final BigDecimal MIN_SUM = new BigDecimal("99.5");
    private static final BigDecimal MAX_SUM = new BigDecimal("100.5");
    private static final String SONSTIGE_ISIN = "SONSTIGE00000";
    private static final String SONSTIGE_NAME = "Sonstige";

    @Override
    public List<AllocationEntry> parseFile(MultipartFile file) {
        log.info("Parsing Xtrackers Excel file: {}", file.getOriginalFilename());

        if (file.isEmpty()) {
            throw new InvalidFileFormatException("Xtrackers", "File is empty");
        }

        // Adjust zip bomb protection to allow highly compressed files
        ZipSecureFile.setMinInflateRatio(0.001);

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            if (sheet == null || sheet.getPhysicalNumberOfRows() < 5) {
                throw new InvalidFileFormatException("Xtrackers", "File does not have enough rows (minimum 5 required)");
            }

            // Validate row 1 contains "DWS" (Xtrackers is part of DWS)
            validateXtrackersFormat(sheet);

            // Headers are in row 4 (index 3)
            Row headerRow = sheet.getRow(3);
            if (headerRow == null) {
                throw new InvalidFileFormatException("Xtrackers", "Header row (row 4) not found");
            }

            // Find column indices
            int nameIndex = findColumnIndex(headerRow, "Name");
            int isinIndex = findColumnIndex(headerRow, "ISIN");
            int weightingIndex = findColumnIndex(headerRow, "Weighting");
            int industryIndex = findColumnIndex(headerRow, "Industry Classification");

            if (nameIndex == -1 || weightingIndex == -1) {
                throw new InvalidFileFormatException("Xtrackers", "Required columns not found: Name, Weighting");
            }

            List<AllocationEntry> entries = new ArrayList<>();
            BigDecimal totalPercentage = BigDecimal.ZERO;
            BigDecimal sonstigePercentage = BigDecimal.ZERO;

            // Parse data rows starting from row 5 (index 4)
            for (int i = 4; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);

                if (row == null || isEmptyRow(row)) {
                    continue;
                }

                String name = getCellValueAsString(row.getCell(nameIndex)).trim();
                String isin = isinIndex != -1 ? getCellValueAsString(row.getCell(isinIndex)).trim() : "";
                String weightingStr = getCellValueAsString(row.getCell(weightingIndex)).trim();
                String industry = industryIndex != -1 ? getCellValueAsString(row.getCell(industryIndex)).trim() : "";

                // Skip if name or weighting is empty or N/A
                if (name.isEmpty() || weightingStr.isEmpty() || weightingStr.equalsIgnoreCase("N/A") || weightingStr.equalsIgnoreCase("-")) {
                    log.warn("Skipping row {}: empty name or weighting", i + 1);
                    continue;
                }

                // Parse percentage - remove % sign and handle comma as decimal separator
                weightingStr = weightingStr.replace("%", "").replace(",", ".").trim();

                try {
                    BigDecimal percentage = new BigDecimal(weightingStr);

                    if (percentage.compareTo(BigDecimal.ZERO) <= 0) {
                        throw new InvalidFileFormatException("Xtrackers",
                            String.format("Percentage must be positive at row %d", i + 1));
                    }

                    if (percentage.compareTo(new BigDecimal("100")) > 0) {
                        throw new InvalidFileFormatException("Xtrackers",
                            String.format("Percentage cannot exceed 100 at row %d", i + 1));
                    }

                    // Check if ISIN is valid (not empty and doesn't start with "_")
                    boolean hasValidIsin = !isin.isEmpty() && !isin.startsWith("_");

                    // Handle entries without valid ISIN - group as "Sonstige"
                    if (!hasValidIsin) {
                        log.debug("Row {} has no valid ISIN, will be grouped as 'Sonstige': {}", i + 1, name);
                        sonstigePercentage = sonstigePercentage.add(percentage);
                        totalPercentage = totalPercentage.add(percentage);
                        continue;
                    }

                    // Extract country code from ISIN (first 2 characters)
                    String countryCode = extractCountryFromIsin(isin);

                    // Map industry classification to GICS standard
                    String mappedSector = mapSectorToGICS(industry);

                    // Track original sector if it couldn't be mapped
                    String originalSector = null;
                    if ("Unbekannt".equals(mappedSector) && !industry.trim().isEmpty()) {
                        originalSector = industry.trim();
                        log.warn("Unmapped sector found for {}: '{}' -> 'Unbekannt'", name, originalSector);
                    }

                    AllocationEntry entry = AllocationEntry.builder()
                        .isin(isin)
                        .name(name)
                        .percentage(percentage)
                        .country(countryCode)
                        .sector(mappedSector)
                        .originalSector(originalSector)
                        .build();

                    entries.add(entry);
                    totalPercentage = totalPercentage.add(percentage);

                } catch (NumberFormatException e) {
                    throw new InvalidFileFormatException("Xtrackers",
                        String.format("Invalid percentage format at row %d: %s", i + 1, weightingStr));
                }
            }

            // Add "Sonstige" entry if there were any entries without valid ISIN
            if (sonstigePercentage.compareTo(BigDecimal.ZERO) > 0) {
                log.info("Adding 'Sonstige' entry with total percentage: {}%", sonstigePercentage);
                AllocationEntry sonstigeEntry = AllocationEntry.builder()
                    .isin(SONSTIGE_ISIN)
                    .name(SONSTIGE_NAME)
                    .percentage(sonstigePercentage)
                    .country(null)
                    .sector(null)
                    .build();
                entries.add(sonstigeEntry);
            }

            if (entries.isEmpty()) {
                throw new InvalidFileFormatException("Xtrackers", "No valid data rows found");
            }

            // Check if percentages are in decimal format (e.g., 0.01 instead of 1)
            // If total is around 1 instead of 100, multiply all percentages by 100
            if (totalPercentage.compareTo(new BigDecimal("2")) < 0) {
                log.info("Detected decimal format percentages, converting to percent format");
                totalPercentage = BigDecimal.ZERO;
                for (AllocationEntry entry : entries) {
                    BigDecimal newPercentage = entry.getPercentage().multiply(new BigDecimal("100"));
                    entry.setPercentage(newPercentage);
                    totalPercentage = totalPercentage.add(newPercentage);
                }
            }

            // Validate total percentage
            if (totalPercentage.compareTo(MIN_SUM) < 0 || totalPercentage.compareTo(MAX_SUM) > 0) {
                throw new AllocationSumException(totalPercentage);
            }

            log.info("Successfully parsed {} allocation entries from Xtrackers Excel, total: {}%", entries.size(), totalPercentage);
            return entries;

        } catch (IOException e) {
            throw new InvalidFileFormatException("Xtrackers", "Failed to read file: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean supports(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !(filename.toLowerCase().endsWith(".xlsx") || filename.toLowerCase().endsWith(".xls"))) {
            return false;
        }

        // Adjust zip bomb protection to allow highly compressed files
        ZipSecureFile.setMinInflateRatio(0.001);

        // Check if file contains "DWS" in row 1 or row 2 - check all cells
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null || sheet.getPhysicalNumberOfRows() == 0) {
                return false;
            }

            // Check rows 1 and 2 (indices 0 and 1)
            for (int rowIndex = 0; rowIndex <= 1; rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }

                // Check all cells in the row for "DWS"
                for (Cell cell : row) {
                    String cellValue = getCellValueAsString(cell);
                    if (cellValue.toLowerCase().contains("dws")) {
                        return true;
                    }
                }
            }

            return false;

        } catch (Exception e) {
            log.debug("Could not check if file is Xtrackers format: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getImporterName() {
        return "Xtrackers Excel Importer";
    }

    /**
     * Validate that row 1 or row 2 contains "DWS" - check all cells in both rows
     */
    private void validateXtrackersFormat(Sheet sheet) {
        boolean foundDWS = false;

        // Check rows 1 and 2 (indices 0 and 1)
        for (int rowIndex = 0; rowIndex <= 1; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }

            // Check all cells in the row for "DWS"
            for (Cell cell : row) {
                String cellValue = getCellValueAsString(cell);
                if (cellValue.toLowerCase().contains("dws")) {
                    foundDWS = true;
                    break;
                }
            }

            if (foundDWS) {
                break;
            }
        }

        if (!foundDWS) {
            throw new InvalidFileFormatException("Xtrackers",
                "This file does not appear to be an Xtrackers holdings file. First rows must contain 'DWS'");
        }
    }

    /**
     * Extract country code from ISIN (first 2 characters)
     */
    private String extractCountryFromIsin(String isin) {
        if (isin == null || isin.length() < 2) {
            return null;
        }
        String countryCode = isin.substring(0, 2).toUpperCase();

        // Validate country code (should be 2 letters)
        if (countryCode.matches("^[A-Z]{2}$")) {
            return countryCode;
        }

        return null;
    }

    /**
     * Check if a row is empty (all cells are empty)
     */
    private boolean isEmptyRow(Row row) {
        if (row == null) {
            return true;
        }

        for (int i = row.getFirstCellNum(); i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            String value = getCellValueAsString(cell).trim();
            if (!value.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    /**
     * Find the index of a column in the header row (case-insensitive)
     */
    private int findColumnIndex(Row headerRow, String... possibleNames) {
        for (Cell cell : headerRow) {
            String cellValue = getCellValueAsString(cell).trim();
            for (String name : possibleNames) {
                if (cellValue.equalsIgnoreCase(name)) {
                    return cell.getColumnIndex();
                }
            }
        }
        return -1;
    }

    /**
     * Get cell value as string, handling different cell types
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                // Format numeric values to avoid scientific notation
                double numericValue = cell.getNumericCellValue();
                if (numericValue == (long) numericValue) {
                    return String.valueOf((long) numericValue);
                }
                return String.valueOf(numericValue);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return String.valueOf(cell.getNumericCellValue());
                } catch (Exception e) {
                    return cell.getStringCellValue();
                }
            case BLANK:
                return "";
            default:
                return "";
        }
    }

    /**
     * Map Xtrackers industry classification to GICS (Global Industry Classification Standard) sectors
     * @param sector The industry classification from Xtrackers data
     * @return GICS-compliant sector name or "Unbekannt" if not mappable
     */
    private String mapSectorToGICS(String sector) {
        if (sector == null || sector.trim().isEmpty()) {
            return "Unbekannt";
        }

        // Normalize sector name for matching
        String normalizedSector = sector.trim().toLowerCase();

        return switch (normalizedSector) {
            // Technology sector
            case "technology", "technologie", "information technology", "informationstechnologie",
                 "tech", "it", "software", "hardware", "semiconductors", "halbleiter" ->
                "Information Technology";

            // Healthcare sector
            case "healthcare", "health care", "gesundheitswesen", "gesundheitsversorgung", "gesundheit", "pharma",
                 "pharmaceuticals", "biotechnology", "biotech", "medical", "medizin" ->
                "Health Care";

            // Financials sector
            case "financials", "financial", "finanzen", "finanzwesen", "finanzdienstleister",
                 "banks", "banken", "insurance", "versicherungen", "financial services" ->
                "Financials";

            // Consumer Discretionary sector
            case "consumer discretionary", "consumer cyclical", "zyklische konsumgüter",
                 "konsumgüter zyklisch", "cyclical consumer goods", "retail", "einzelhandel",
                 "consumer goods cyclical", "cyclical consumer goods & services",
                 "verbrauchsgüter" ->
                "Consumer Discretionary";

            // Consumer Staples sector
            case "consumer staples", "consumer defensive", "basiskonsumgüter", "nicht-zyklische konsumgüter",
                 "nichtzyklische konsumgüter", "konsumgüter nicht-zyklisch", "non-cyclical consumer goods",
                 "food & beverage", "consumer goods - defensive", "consumer goods & services" ->
                "Consumer Staples";

            // Industrials sector
            case "industrials", "industrial", "industrie", "industriewerte", "industrieunternehmen",
                 "machinery", "maschinen", "transportation", "transport" ->
                "Industrials";

            // Energy sector
            case "energy", "energie", "oil", "öl", "gas", "oil & gas", "petroleum" ->
                "Energy";

            // Materials sector
            case "materials", "basic materials", "rohstoffe", "grundstoffe", "materialien",
                 "chemicals", "chemie", "metals", "metalle", "mining", "bergbau" ->
                "Materials";

            // Real Estate sector
            case "real estate", "immobilien", "reits", "property" ->
                "Real Estate";

            // Utilities sector
            case "utilities", "versorgungsbetriebe", "versorgungsunternehmen", "versorger", "utility" ->
                "Utilities";

            // Communication Services sector
            case "communication services", "communications", "kommunikationsdienste", "kommunikation",
                 "telekommunikation", "telecommunication", "telecom", "media", "medien" ->
                "Communication Services";

            // Unknown/Other
            default -> {
                log.debug("Unmapped sector '{}' - using 'Unbekannt'", sector);
                yield "Unbekannt";
            }
        };
    }
}
