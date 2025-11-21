package com.stockstatus.service.importer;

import com.stockstatus.dto.AllocationEntry;
import com.stockstatus.exception.AllocationSumException;
import com.stockstatus.exception.InvalidFileFormatException;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Generic Excel importer that expects columns: ISIN, Name, Percentage
 * Optional columns: Country, Sector
 * Supports .xlsx format
 */
@Component
@Slf4j
public class GenericExcelImporter implements FileImporter {

    private static final BigDecimal MIN_SUM = new BigDecimal("99.5");
    private static final BigDecimal MAX_SUM = new BigDecimal("100.5");

    @Override
    public List<AllocationEntry> parseFile(MultipartFile file) {
        log.info("Parsing Excel file: {}", file.getOriginalFilename());

        if (file.isEmpty()) {
            throw new InvalidFileFormatException("Excel", "File is empty");
        }

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            if (sheet == null || sheet.getPhysicalNumberOfRows() == 0) {
                throw new InvalidFileFormatException("Excel", "No data found in file");
            }

            // Parse header to find column indices
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) {
                throw new InvalidFileFormatException("Excel", "No header row found");
            }

            int isinIndex = findColumnIndex(headerRow, "ISIN");
            int nameIndex = findColumnIndex(headerRow, "Name");
            int percentageIndex = findColumnIndex(headerRow, "Percentage", "Weight", "Percent");
            int countryIndex = findColumnIndex(headerRow, "Country");
            int sectorIndex = findColumnIndex(headerRow, "Sector");

            if (isinIndex == -1 || nameIndex == -1 || percentageIndex == -1) {
                throw new InvalidFileFormatException("Excel", "Required columns not found: ISIN, Name, Percentage");
            }

            List<AllocationEntry> entries = new ArrayList<>();
            BigDecimal totalPercentage = BigDecimal.ZERO;

            // Parse data rows (skip header)
            for (int i = sheet.getFirstRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);

                if (row == null) {
                    continue;
                }

                String isin = getCellValueAsString(row.getCell(isinIndex));
                String name = getCellValueAsString(row.getCell(nameIndex));
                String percentageStr = getCellValueAsString(row.getCell(percentageIndex));

                if (isin.isEmpty() || name.isEmpty() || percentageStr.isEmpty()) {
                    log.warn("Skipping row {}: empty required field", i + 1);
                    continue;
                }

                // Remove % sign and handle comma as decimal separator
                percentageStr = percentageStr.replace("%", "").replace(",", ".").trim();

                try {
                    BigDecimal percentage = new BigDecimal(percentageStr);

                    if (percentage.compareTo(BigDecimal.ZERO) <= 0) {
                        throw new InvalidFileFormatException("Excel",
                            String.format("Percentage must be positive at row %d", i + 1));
                    }

                    if (percentage.compareTo(new BigDecimal("100")) > 0) {
                        throw new InvalidFileFormatException("Excel",
                            String.format("Percentage cannot exceed 100 at row %d", i + 1));
                    }

                    AllocationEntry.AllocationEntryBuilder builder = AllocationEntry.builder()
                        .isin(isin.trim())
                        .name(name.trim())
                        .percentage(percentage);

                    // Add optional fields if present
                    if (countryIndex != -1) {
                        String country = getCellValueAsString(row.getCell(countryIndex));
                        if (!country.isEmpty()) {
                            builder.country(country.trim());
                        }
                    }

                    if (sectorIndex != -1) {
                        String sector = getCellValueAsString(row.getCell(sectorIndex));
                        if (!sector.isEmpty()) {
                            builder.sector(sector.trim());
                        }
                    }

                    AllocationEntry entry = builder.build();
                    entries.add(entry);
                    totalPercentage = totalPercentage.add(percentage);

                } catch (NumberFormatException e) {
                    throw new InvalidFileFormatException("Excel",
                        String.format("Invalid percentage format at row %d: %s", i + 1, percentageStr));
                }
            }

            if (entries.isEmpty()) {
                throw new InvalidFileFormatException("Excel", "No valid data rows found");
            }

            // Validate total percentage
            if (totalPercentage.compareTo(MIN_SUM) < 0 || totalPercentage.compareTo(MAX_SUM) > 0) {
                throw new AllocationSumException(totalPercentage);
            }

            log.info("Successfully parsed {} allocation entries from Excel, total: {}%", entries.size(), totalPercentage);
            return entries;

        } catch (IOException e) {
            throw new InvalidFileFormatException("Excel", "Failed to read file: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean supports(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }
        String filename = file.getOriginalFilename();
        return filename != null && (filename.toLowerCase().endsWith(".xlsx") || filename.toLowerCase().endsWith(".xls"));
    }

    @Override
    public String getImporterName() {
        return "Generic Excel Importer";
    }

    /**
     * Find the index of a column in the header row (case-insensitive)
     * @param headerRow the header row
     * @param possibleNames possible column names to search for
     * @return column index or -1 if not found
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
     * @param cell the cell to read
     * @return cell value as string, or empty string if cell is null
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
