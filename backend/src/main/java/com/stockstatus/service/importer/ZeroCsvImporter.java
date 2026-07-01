package com.stockstatus.service.importer;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import com.stockstatus.dto.PortfolioValueEntry;
import com.stockstatus.exception.InvalidFileFormatException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Importer for the ZERO broker portfolio CSV export.
 * Expected format:
 * - Semicolon (';') separated, UTF-8 (optionally with BOM)
 * - Header row: Name;ISIN;WKN;Art;Anzahl;Verfügbar;Kaufkurs;Kaufwert;Kurs;Kurszeit;Kursdatum;Wert;Erfolg [%];Erfolg [EUR];Notiz
 * - German number format: dot as thousands separator, comma as decimal (e.g. "1.636,13")
 *
 * Only the "ISIN" and "Wert" (current value in EUR) columns are used. The parsed
 * value is rounded to full euros.
 */
@Component
@Slf4j
public class ZeroCsvImporter {

    private static final char SEPARATOR = ';';
    private static final String BOM = "﻿";
    private static final String ISIN_COLUMN = "ISIN";
    private static final String VALUE_COLUMN = "Wert";

    /**
     * Parse the CSV file into a list of ISIN -> rounded value entries.
     */
    public List<PortfolioValueEntry> parseFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileFormatException("ZERO", "File is empty");
        }

        log.info("Parsing ZERO CSV file: {}", file.getOriginalFilename());

        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
             CSVReader csvReader = new CSVReaderBuilder(reader)
                 .withCSVParser(new CSVParserBuilder().withSeparator(SEPARATOR).build())
                 .build()) {

            List<String[]> rows = csvReader.readAll();
            if (rows.isEmpty()) {
                throw new InvalidFileFormatException("ZERO", "File contains no rows");
            }

            String[] header = rows.get(0);
            stripBom(header);

            int isinIndex = findColumnIndex(header, ISIN_COLUMN);
            int valueIndex = findColumnIndex(header, VALUE_COLUMN);

            if (isinIndex == -1 || valueIndex == -1) {
                throw new InvalidFileFormatException("ZERO",
                    "Required columns not found: " + ISIN_COLUMN + ", " + VALUE_COLUMN);
            }

            List<PortfolioValueEntry> entries = new ArrayList<>();
            for (int i = 1; i < rows.size(); i++) {
                String[] row = rows.get(i);
                if (row.length <= Math.max(isinIndex, valueIndex)) {
                    continue;
                }

                String isin = safeTrim(row[isinIndex]);
                String valueStr = safeTrim(row[valueIndex]);

                if (isin.isEmpty() || valueStr.isEmpty()) {
                    continue;
                }

                BigDecimal value = parseGermanValue(valueStr, i + 1);
                entries.add(new PortfolioValueEntry(isin, value));
            }

            if (entries.isEmpty()) {
                throw new InvalidFileFormatException("ZERO", "No valid data rows found");
            }

            log.info("Parsed {} value entries from ZERO CSV", entries.size());
            return entries;

        } catch (IOException | CsvException e) {
            throw new InvalidFileFormatException("ZERO", "Failed to read file: " + e.getMessage(), e);
        }
    }

    /**
     * Parse a German-formatted euro amount and round to full euros.
     * Dot is the thousands separator, comma is the decimal separator.
     */
    private BigDecimal parseGermanValue(String raw, int rowNumber) {
        String normalized = raw.replace(".", "").replace(",", ".").trim();
        try {
            return new BigDecimal(normalized).setScale(0, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            throw new InvalidFileFormatException("ZERO",
                String.format("Invalid value format at row %d: %s", rowNumber, raw));
        }
    }

    private void stripBom(String[] header) {
        if (header.length > 0 && header[0] != null && header[0].startsWith(BOM)) {
            header[0] = header[0].substring(BOM.length());
        }
    }

    private int findColumnIndex(String[] header, String name) {
        for (int i = 0; i < header.length; i++) {
            if (header[i] != null && header[i].trim().equalsIgnoreCase(name)) {
                return i;
            }
        }
        return -1;
    }

    private String safeTrim(String value) {
        return value != null ? value.trim() : "";
    }
}
