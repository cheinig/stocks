package com.stockstatus.service.importer;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import com.stockstatus.dto.AllocationEntry;
import com.stockstatus.exception.AllocationSumException;
import com.stockstatus.exception.InvalidFileFormatException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Generic CSV importer that expects columns: ISIN, Name, Percentage
 * Optional columns: Country, Sector
 */
@Component
@Slf4j
public class GenericCSVImporter implements FileImporter {

    private static final BigDecimal TOLERANCE = new BigDecimal("0.5"); // 0.5% tolerance
    private static final BigDecimal MIN_SUM = new BigDecimal("99.5");
    private static final BigDecimal MAX_SUM = new BigDecimal("100.5");

    @Override
    public List<AllocationEntry> parseFile(MultipartFile file) {
        log.info("Parsing CSV file: {}", file.getOriginalFilename());

        if (file.isEmpty()) {
            throw new InvalidFileFormatException("CSV", "File is empty");
        }

        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            List<String[]> rows = reader.readAll();

            if (rows.isEmpty()) {
                throw new InvalidFileFormatException("CSV", "No data found in file");
            }

            // Parse header to find column indices
            String[] header = rows.get(0);
            int isinIndex = findColumnIndex(header, "ISIN");
            int nameIndex = findColumnIndex(header, "Name");
            int percentageIndex = findColumnIndex(header, "Percentage", "Weight", "Percent");
            int countryIndex = findColumnIndex(header, "Country");
            int sectorIndex = findColumnIndex(header, "Sector");

            if (isinIndex == -1 || nameIndex == -1 || percentageIndex == -1) {
                throw new InvalidFileFormatException("CSV", "Required columns not found: ISIN, Name, Percentage");
            }

            List<AllocationEntry> entries = new ArrayList<>();
            BigDecimal totalPercentage = BigDecimal.ZERO;

            // Parse data rows (skip header)
            for (int i = 1; i < rows.size(); i++) {
                String[] row = rows.get(i);

                if (row.length <= Math.max(isinIndex, Math.max(nameIndex, percentageIndex))) {
                    log.warn("Skipping row {}: insufficient columns", i + 1);
                    continue;
                }

                String isin = row[isinIndex].trim();
                String name = row[nameIndex].trim();
                String percentageStr = row[percentageIndex].trim().replace("%", "").replace(",", ".");

                if (isin.isEmpty() || name.isEmpty() || percentageStr.isEmpty()) {
                    log.warn("Skipping row {}: empty required field", i + 1);
                    continue;
                }

                try {
                    BigDecimal percentage = new BigDecimal(percentageStr);

                    if (percentage.compareTo(BigDecimal.ZERO) <= 0) {
                        throw new InvalidFileFormatException("CSV",
                            String.format("Percentage must be positive at row %d", i + 1));
                    }

                    if (percentage.compareTo(new BigDecimal("100")) > 0) {
                        throw new InvalidFileFormatException("CSV",
                            String.format("Percentage cannot exceed 100 at row %d", i + 1));
                    }

                    AllocationEntry.AllocationEntryBuilder builder = AllocationEntry.builder()
                        .isin(isin)
                        .name(name)
                        .percentage(percentage);

                    // Add optional fields if present
                    if (countryIndex != -1 && countryIndex < row.length && !row[countryIndex].trim().isEmpty()) {
                        builder.country(row[countryIndex].trim());
                    }

                    if (sectorIndex != -1 && sectorIndex < row.length && !row[sectorIndex].trim().isEmpty()) {
                        builder.sector(row[sectorIndex].trim());
                    }

                    AllocationEntry entry = builder.build();
                    entries.add(entry);
                    totalPercentage = totalPercentage.add(percentage);

                } catch (NumberFormatException e) {
                    throw new InvalidFileFormatException("CSV",
                        String.format("Invalid percentage format at row %d: %s", i + 1, percentageStr));
                }
            }

            if (entries.isEmpty()) {
                throw new InvalidFileFormatException("CSV", "No valid data rows found");
            }

            // Validate total percentage
            if (totalPercentage.compareTo(MIN_SUM) < 0 || totalPercentage.compareTo(MAX_SUM) > 0) {
                throw new AllocationSumException(totalPercentage);
            }

            log.info("Successfully parsed {} allocation entries from CSV, total: {}%", entries.size(), totalPercentage);
            return entries;

        } catch (IOException e) {
            throw new InvalidFileFormatException("CSV", "Failed to read file: " + e.getMessage(), e);
        } catch (CsvException e) {
            throw new InvalidFileFormatException("CSV", "Failed to parse CSV: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean supports(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }
        String filename = file.getOriginalFilename();
        return filename != null && filename.toLowerCase().endsWith(".csv");
    }

    @Override
    public String getImporterName() {
        return "Generic CSV Importer";
    }

    /**
     * Find the index of a column in the header row (case-insensitive)
     * @param header the header row
     * @param possibleNames possible column names to search for
     * @return column index or -1 if not found
     */
    private int findColumnIndex(String[] header, String... possibleNames) {
        for (int i = 0; i < header.length; i++) {
            String col = header[i].trim();
            for (String name : possibleNames) {
                if (col.equalsIgnoreCase(name)) {
                    return i;
                }
            }
        }
        return -1;
    }
}
