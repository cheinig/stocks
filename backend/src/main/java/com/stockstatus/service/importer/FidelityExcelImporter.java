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
 * Fidelity Excel importer for ETF holdings
 * Expected format:
 * - Row 1: Title containing "Fidelity"
 * - Row 2: Date row
 * - Row 3: Headers (ISIN | Name | Gewichtung (%))
 * - Row 4+: Data rows
 *
 * Special handling:
 * - Country code is extracted from first 2 characters of ISIN
 * - Sector is left empty
 * - Entries without ISIN are grouped as "Sonstige"
 */
@Component
@Slf4j
public class FidelityExcelImporter implements FileImporter {

    private static final BigDecimal MIN_SUM = new BigDecimal("99.5");
    private static final BigDecimal MAX_SUM = new BigDecimal("100.5");
    private static final String SONSTIGE_ISIN = "SONSTIGE00000";
    private static final String SONSTIGE_NAME = "Sonstige";

    @Override
    public List<AllocationEntry> parseFile(MultipartFile file) {
        log.info("Parsing Fidelity Excel file: {}", file.getOriginalFilename());

        if (file.isEmpty()) {
            throw new InvalidFileFormatException("Fidelity", "File is empty");
        }

        // Adjust zip bomb protection to allow highly compressed files
        ZipSecureFile.setMinInflateRatio(0.001);

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            if (sheet == null || sheet.getPhysicalNumberOfRows() < 4) {
                throw new InvalidFileFormatException("Fidelity", "File does not have enough rows (minimum 4 required)");
            }

            // Validate row 1 contains "Fidelity"
            validateFidelityFormat(sheet);

            // Headers are in row 3 (index 2)
            Row headerRow = sheet.getRow(2);
            if (headerRow == null) {
                throw new InvalidFileFormatException("Fidelity", "Header row (row 3) not found");
            }

            // Find column indices
            int isinIndex = findColumnIndex(headerRow, "ISIN");
            int nameIndex = findColumnIndex(headerRow, "Name");
            int percentageIndex = findColumnIndex(headerRow, "Gewichtung", "Weight", "Percentage");
            int sectorIndex = findColumnIndex(headerRow, "Sector", "Sektor", "Branche");

            if (nameIndex == -1 || percentageIndex == -1) {
                throw new InvalidFileFormatException("Fidelity", "Required columns not found: Name, Gewichtung (%)");
            }

            List<AllocationEntry> entries = new ArrayList<>();
            BigDecimal totalPercentage = BigDecimal.ZERO;
            BigDecimal sonstigePercentage = BigDecimal.ZERO;

            // Parse data rows starting from row 4 (index 3)
            for (int i = 3; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);

                if (row == null || isEmptyRow(row)) {
                    continue;
                }

                String isin = isinIndex != -1 ? getCellValueAsString(row.getCell(isinIndex)).trim() : "";
                String name = getCellValueAsString(row.getCell(nameIndex)).trim();
                String percentageStr = getCellValueAsString(row.getCell(percentageIndex)).trim();
                String sector = sectorIndex != -1 ? getCellValueAsString(row.getCell(sectorIndex)).trim() : "";

                // Skip if name or percentage is empty
                if (name.isEmpty() || percentageStr.isEmpty()) {
                    log.warn("Skipping row {}: empty name or percentage", i + 1);
                    continue;
                }

                // Remove % sign and handle comma as decimal separator
                percentageStr = percentageStr.replace("%", "").replace(",", ".").trim();

                try {
                    BigDecimal percentage = new BigDecimal(percentageStr);

                    if (percentage.compareTo(new BigDecimal("0.000001")) < 0) {
                        throw new InvalidFileFormatException("Fidelity",
                            String.format("Percentage must be positive at row %d", i + 1));
                    }

                    if (percentage.compareTo(new BigDecimal("100")) > 0) {
                        throw new InvalidFileFormatException("Fidelity",
                            String.format("Percentage cannot exceed 100 at row %d", i + 1));
                    }

                    // Handle entries without ISIN - group as "Sonstige"
                    if (isin.isEmpty()) {
                        log.debug("Row {} has no ISIN, will be grouped as 'Sonstige': {}", i + 1, name);
                        sonstigePercentage = sonstigePercentage.add(percentage);
                        totalPercentage = totalPercentage.add(percentage);
                        continue;
                    }

                    // Extract country code from ISIN (first 2 characters)
                    String countryCode = extractCountryFromIsin(isin);

                    // Map sector to GICS standard
                    String mappedSector = mapSectorToGICS(sector);

                    AllocationEntry entry = AllocationEntry.builder()
                        .isin(isin)
                        .name(name)
                        .percentage(percentage)
                        .country(countryCode)
                        .sector(mappedSector)
                        .originalSector("Unbekannt".equals(mappedSector) && !sector.trim().isEmpty() ? sector.trim() : null)
                        .build();

                    entries.add(entry);
                    totalPercentage = totalPercentage.add(percentage);

                } catch (NumberFormatException e) {
                    throw new InvalidFileFormatException("Fidelity",
                        String.format("Invalid percentage format at row %d: %s", i + 1, percentageStr));
                }
            }

            // Add "Sonstige" entry if there were any entries without ISIN
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
                throw new InvalidFileFormatException("Fidelity", "No valid data rows found");
            }

            // Validate total percentage
            if (totalPercentage.compareTo(MIN_SUM) < 0 || totalPercentage.compareTo(MAX_SUM) > 0) {
                throw new AllocationSumException(totalPercentage);
            }

            log.info("Successfully parsed {} allocation entries from Fidelity Excel, total: {}%", entries.size(), totalPercentage);
            return entries;

        } catch (IOException e) {
            throw new InvalidFileFormatException("Fidelity", "Failed to read file: " + e.getMessage(), e);
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

        // Check if file contains "Fidelity" in row 1
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null || sheet.getPhysicalNumberOfRows() == 0) {
                return false;
            }

            Row firstRow = sheet.getRow(0);
            if (firstRow == null) {
                return false;
            }

            String firstCellValue = getCellValueAsString(firstRow.getCell(0));
            return firstCellValue.toLowerCase().contains("fidelity");

        } catch (Exception e) {
            log.debug("Could not check if file is Fidelity format: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getImporterName() {
        return "Fidelity Excel Importer";
    }

    /**
     * Validate that row 1 contains "Fidelity"
     */
    private void validateFidelityFormat(Sheet sheet) {
        Row firstRow = sheet.getRow(0);
        if (firstRow == null) {
            throw new InvalidFileFormatException("Fidelity", "Row 1 not found");
        }

        String firstCellValue = getCellValueAsString(firstRow.getCell(0));
        if (!firstCellValue.toLowerCase().contains("fidelity")) {
            throw new InvalidFileFormatException("Fidelity",
                "This file does not appear to be a Fidelity holdings file. Row 1 must contain 'Fidelity'");
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
                if (cellValue.toLowerCase().contains(name.toLowerCase())) {
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
     * Map Fidelity sector names to GICS (Global Industry Classification Standard) sectors
     * @param sector The sector name from Fidelity data
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
            case "financials", "financial", "finanzen", "finanzwesen", "banks", "banken",
                 "insurance", "versicherungen", "financial services" ->
                "Financials";

            // Consumer Discretionary sector
            case "consumer discretionary", "consumer cyclical", "zyklische konsumgüter",
                 "konsumgüter zyklisch", "cyclical consumer goods", "retail", "einzelhandel" ->
                "Consumer Discretionary";

            // Consumer Staples sector
            case "consumer staples", "consumer defensive", "basiskonsumgüter", "nicht-zyklische konsumgüter",
                 "nichtzyklische konsumgüter", "konsumgüter nicht-zyklisch", "non-cyclical consumer goods",
                 "food & beverage" ->
                "Consumer Staples";

            // Industrials sector
            case "industrials", "industrial", "industrie", "industriewerte", "machinery",
                 "maschinen", "transportation", "transport" ->
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
            case "utilities", "versorgungsbetriebe", "versorger", "utility" ->
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
