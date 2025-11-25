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
 * VanEck Excel importer for ETF holdings
 * Expected format:
 * - Row 1: "All Holdings" with date (e.g., "All Holdings 10/28/2025")
 * - Row 2: Empty
 * - Row 3: Headers (Number | Holding Name | Ticker | ISIN | Shares | Market Value | % of Net Assets)
 * - Row 4+: Data rows
 *
 * Special handling:
 * - ISIN column is used for stock identification
 * - "% of Net Assets" column contains the percentage (may be in format like "4.65%")
 * - Entries without valid ISIN (empty or starting with "_") are grouped as "Sonstige"
 */
@Component
@Slf4j
public class VanEckExcelImporter implements FileImporter {

    private static final BigDecimal MIN_SUM = new BigDecimal("99.5");
    private static final BigDecimal MAX_SUM = new BigDecimal("100.5");
    private static final String SONSTIGE_ISIN = "SONSTIGE00000";
    private static final String SONSTIGE_NAME = "Sonstige";

    @Override
    public List<AllocationEntry> parseFile(MultipartFile file) {
        log.info("Parsing VanEck Excel file: {}", file.getOriginalFilename());

        if (file.isEmpty()) {
            throw new InvalidFileFormatException("VanEck", "File is empty");
        }

        // Adjust zip bomb protection to allow highly compressed files
        ZipSecureFile.setMinInflateRatio(0.001);

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            if (sheet == null || sheet.getPhysicalNumberOfRows() < 4) {
                throw new InvalidFileFormatException("VanEck", "File does not have enough rows (minimum 4 required)");
            }

            // Validate row 1 contains "All Holdings"
            validateVanEckFormat(sheet);

            // Headers are in row 3 (index 2)
            Row headerRow = sheet.getRow(2);
            if (headerRow == null) {
                throw new InvalidFileFormatException("VanEck", "Header row (row 3) not found");
            }

            // Find column indices
            int nameIndex = findColumnIndex(headerRow, "Holding Name", "Name");
            int isinIndex = findColumnIndex(headerRow, "ISIN");
            int percentageIndex = findColumnIndex(headerRow, "% of Net Assets", "% Net Assets");

            if (nameIndex == -1 || percentageIndex == -1) {
                throw new InvalidFileFormatException("VanEck", "Required columns not found: Holding Name, % of Net Assets");
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

                String name = getCellValueAsString(row.getCell(nameIndex)).trim();
                String isin = isinIndex != -1 ? getCellValueAsString(row.getCell(isinIndex)).trim() : "";
                String percentageStr = getCellValueAsString(row.getCell(percentageIndex)).trim();

                // Skip if name or percentage is empty or N/A
                if (name.isEmpty() || percentageStr.isEmpty() || percentageStr.equalsIgnoreCase("N/A") || percentageStr.equalsIgnoreCase("-")) {
                    log.warn("Skipping row {}: empty name or percentage", i + 1);
                    continue;
                }

                // Parse percentage - remove % sign and handle comma as decimal separator
                percentageStr = percentageStr.replace("%", "").replace(",", ".").trim();

                try {
                    BigDecimal percentage = new BigDecimal(percentageStr);

                    if (percentage.compareTo(BigDecimal.ZERO) <= 0) {
                        throw new InvalidFileFormatException("VanEck",
                            String.format("Percentage must be positive at row %d", i + 1));
                    }

                    if (percentage.compareTo(new BigDecimal("100")) > 0) {
                        throw new InvalidFileFormatException("VanEck",
                            String.format("Percentage cannot exceed 100 at row %d", i + 1));
                    }

                    // Check if ISIN is valid (not empty, doesn't start with "_", and not "--")
                    boolean hasValidIsin = !isin.isEmpty() && !isin.startsWith("_") && !isin.equals("--");

                    // Handle entries without valid ISIN - group as "Sonstige"
                    if (!hasValidIsin) {
                        log.debug("Row {} has no valid ISIN, will be grouped as 'Sonstige': {}", i + 1, name);
                        sonstigePercentage = sonstigePercentage.add(percentage);
                        totalPercentage = totalPercentage.add(percentage);
                        continue;
                    }

                    // Extract country code from ISIN (first 2 characters)
                    String countryCode = extractCountryFromIsin(isin);

                    AllocationEntry entry = AllocationEntry.builder()
                        .isin(isin)
                        .name(name)
                        .percentage(percentage)
                        .country(countryCode)
                        .sector(null) // VanEck format doesn't include sector information
                        .build();

                    entries.add(entry);
                    totalPercentage = totalPercentage.add(percentage);

                } catch (NumberFormatException e) {
                    throw new InvalidFileFormatException("VanEck",
                        String.format("Invalid percentage format at row %d: %s", i + 1, percentageStr));
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
                throw new InvalidFileFormatException("VanEck", "No valid data rows found");
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

            log.info("Successfully parsed {} allocation entries from VanEck Excel, total: {}%", entries.size(), totalPercentage);
            return entries;

        } catch (IOException e) {
            throw new InvalidFileFormatException("VanEck", "Failed to read file: " + e.getMessage(), e);
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

        // Check if file contains "All Holdings" in row 1
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null || sheet.getPhysicalNumberOfRows() == 0) {
                return false;
            }

            // Check row 1 (index 0)
            Row row = sheet.getRow(0);
            if (row == null) {
                return false;
            }

            // Check all cells in the row for "All Holdings"
            for (Cell cell : row) {
                String cellValue = getCellValueAsString(cell);
                if (cellValue.toLowerCase().contains("all holdings")) {
                    return true;
                }
            }

            return false;

        } catch (Exception e) {
            log.debug("Could not check if file is VanEck format: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getImporterName() {
        return "VanEck Excel Importer";
    }

    /**
     * Validate that row 1 contains "All Holdings"
     */
    private void validateVanEckFormat(Sheet sheet) {
        Row row = sheet.getRow(0);
        if (row == null) {
            throw new InvalidFileFormatException("VanEck",
                "This file does not appear to be a VanEck holdings file. First row is empty");
        }

        boolean foundAllHoldings = false;

        // Check all cells in the row for "All Holdings"
        for (Cell cell : row) {
            String cellValue = getCellValueAsString(cell);
            if (cellValue.toLowerCase().contains("all holdings")) {
                foundAllHoldings = true;
                break;
            }
        }

        if (!foundAllHoldings) {
            throw new InvalidFileFormatException("VanEck",
                "This file does not appear to be a VanEck holdings file. First row must contain 'All Holdings'");
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
}
