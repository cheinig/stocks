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
 * Amundi Excel importer for ETF holdings
 * Expected format:
 * - Rows 1-18: Header section with ETF information (title, ISIN, etc.)
 * - Row 20: Column headers (ISIN | Name | Anlageklasse | Währung | Gewichtung | Sektor | Land)
 * - Row 21+: Data rows
 * - Bottom section: Legal disclaimers and footer
 *
 * Special handling:
 * - ISIN column is used for stock identification
 * - "Gewichtung" column contains the percentage (format like "4,78%" or "4.78%")
 * - "Land" column contains country code (e.g., "USA", "Germany")
 * - "Sektor" column contains sector information
 * - Entries without valid ISIN (empty or starting with "_") are grouped as "Sonstige"
 */
@Component
@Slf4j
public class AmundiExcelImporter implements FileImporter {

    private static final BigDecimal MIN_SUM = new BigDecimal("99.0");
    private static final BigDecimal MAX_SUM = new BigDecimal("100.5");
    private static final String SONSTIGE_ISIN = "SONSTIGE00000";
    private static final String SONSTIGE_NAME = "Sonstige";

    @Override
    public List<AllocationEntry> parseFile(MultipartFile file) {
        log.info("Parsing Amundi Excel file: {}", file.getOriginalFilename());

        if (file.isEmpty()) {
            throw new InvalidFileFormatException("Amundi", "File is empty");
        }

        // Adjust zip bomb protection to allow highly compressed files
        ZipSecureFile.setMinInflateRatio(0.001);

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            if (sheet == null || sheet.getPhysicalNumberOfRows() < 21) {
                throw new InvalidFileFormatException("Amundi", "File does not have enough rows (minimum 21 required)");
            }

            // Validate Amundi format
            validateAmundiFormat(sheet);

            // Find header row - should be around row 20 (index 19)
            int headerRowIndex = findHeaderRow(sheet);
            if (headerRowIndex == -1) {
                throw new InvalidFileFormatException("Amundi", "Could not find header row with columns ISIN, Name, Gewichtung");
            }

            Row headerRow = sheet.getRow(headerRowIndex);
            if (headerRow == null) {
                throw new InvalidFileFormatException("Amundi", "Header row not found");
            }

            // Find column indices
            int isinIndex = findColumnIndex(headerRow, "ISIN");
            int nameIndex = findColumnIndex(headerRow, "Name");
            int percentageIndex = findColumnIndex(headerRow, "Gewichtung", "Allocation", "Weight", "%");
            int sectorIndex = findColumnIndex(headerRow, "Sektor", "Sector");
            int countryIndex = findColumnIndex(headerRow, "Land", "Country");

            if (isinIndex == -1 || nameIndex == -1 || percentageIndex == -1) {
                throw new InvalidFileFormatException("Amundi", "Required columns not found: ISIN, Name, Gewichtung");
            }

            List<AllocationEntry> entries = new ArrayList<>();
            BigDecimal totalPercentage = BigDecimal.ZERO;
            BigDecimal sonstigePercentage = BigDecimal.ZERO;

            // Parse data rows starting from the row after header
            for (int i = headerRowIndex + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);

                if (row == null || isEmptyRow(row)) {
                    continue;
                }

                String isin = getCellValueAsString(row.getCell(isinIndex)).trim();
                String name = getCellValueAsString(row.getCell(nameIndex)).trim();
                String percentageStr = getCellValueAsString(row.getCell(percentageIndex)).trim();
                String sector = sectorIndex != -1 ? getCellValueAsString(row.getCell(sectorIndex)).trim() : "";
                String country = countryIndex != -1 ? getCellValueAsString(row.getCell(countryIndex)).trim() : "";

                // Skip if name or percentage is empty or N/A
                if (name.isEmpty() || percentageStr.isEmpty() || percentageStr.equalsIgnoreCase("N/A") || percentageStr.equalsIgnoreCase("-")) {
                    log.debug("Skipping row {}: empty name or percentage", i + 1);
                    continue;
                }

                // Check if this is the footer section - stop parsing if we encounter "Quelle:" or long text blocks
                if (name.toLowerCase().startsWith("quelle:") || name.length() > 100) {
                    log.debug("Reached footer section at row {}, stopping data parsing", i + 1);
                    break;
                }

                // Parse percentage - remove % sign and handle comma as decimal separator
                percentageStr = percentageStr.replace("%", "").replace(",", ".").trim();

                try {
                    BigDecimal percentage = new BigDecimal(percentageStr);

                    if (percentage.compareTo(BigDecimal.ZERO) <= 0) {
                        throw new InvalidFileFormatException("Amundi",
                            String.format("Percentage must be positive at row %d", i + 1));
                    }

                    if (percentage.compareTo(new BigDecimal("100")) > 0) {
                        throw new InvalidFileFormatException("Amundi",
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

                    // Extract country code from ISIN if not provided in Land column
                    String countryCode = null;
                    if (country != null && !country.isEmpty()) {
                        countryCode = mapCountryNameToCode(country);
                    }
                    if (countryCode == null) {
                        countryCode = extractCountryFromIsin(isin);
                    }

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
                    throw new InvalidFileFormatException("Amundi",
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
                throw new InvalidFileFormatException("Amundi", "No valid data rows found");
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

            log.info("Successfully parsed {} allocation entries from Amundi Excel, total: {}%", entries.size(), totalPercentage);
            return entries;

        } catch (IOException e) {
            throw new InvalidFileFormatException("Amundi", "Failed to read file: " + e.getMessage(), e);
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

        // Check if file contains "Amundi" in the first few rows
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null || sheet.getPhysicalNumberOfRows() < 10) {
                return false;
            }

            // Check first 10 rows for "Amundi" identifier
            for (int i = 0; i < Math.min(10, sheet.getLastRowNum() + 1); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }

                // Check all cells in the row for "Amundi"
                for (Cell cell : row) {
                    String cellValue = getCellValueAsString(cell);
                    if (cellValue.toLowerCase().contains("amundi")) {
                        return true;
                    }
                }
            }

            return false;

        } catch (Exception e) {
            log.debug("Could not check if file is Amundi format: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getImporterName() {
        return "Amundi Excel Importer";
    }

    /**
     * Validate that the file appears to be an Amundi holdings file
     */
    private void validateAmundiFormat(Sheet sheet) {
        boolean foundAmundi = false;

        // Check first 10 rows for "Amundi"
        for (int i = 0; i < Math.min(10, sheet.getLastRowNum() + 1); i++) {
            Row row = sheet.getRow(i);
            if (row == null) {
                continue;
            }

            for (Cell cell : row) {
                String cellValue = getCellValueAsString(cell);
                if (cellValue.toLowerCase().contains("amundi")) {
                    foundAmundi = true;
                    break;
                }
            }

            if (foundAmundi) {
                break;
            }
        }

        if (!foundAmundi) {
            throw new InvalidFileFormatException("Amundi",
                "This file does not appear to be an Amundi holdings file. First rows must contain 'Amundi'");
        }
    }

    /**
     * Find the header row containing ISIN, Name, and Gewichtung columns
     */
    private int findHeaderRow(Sheet sheet) {
        // Check rows 15-25 for header (typically around row 20)
        for (int i = 15; i <= Math.min(25, sheet.getLastRowNum()); i++) {
            Row row = sheet.getRow(i);
            if (row == null) {
                continue;
            }

            boolean hasIsin = false;
            boolean hasName = false;
            boolean hasGewichtung = false;

            for (Cell cell : row) {
                String cellValue = getCellValueAsString(cell).trim().toLowerCase();
                if (cellValue.equals("isin")) {
                    hasIsin = true;
                } else if (cellValue.equals("name")) {
                    hasName = true;
                } else if (cellValue.contains("gewichtung") || cellValue.contains("weight") || cellValue.equals("%")) {
                    hasGewichtung = true;
                }
            }

            if (hasIsin && hasName && hasGewichtung) {
                log.debug("Found header row at index {}", i);
                return i;
            }
        }

        return -1;
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
     * Map country name to ISO 3166-1 alpha-2 code
     */
    private String mapCountryNameToCode(String countryName) {
        if (countryName == null || countryName.trim().isEmpty()) {
            return null;
        }

        String normalized = countryName.trim().toUpperCase();

        return switch (normalized) {
            case "USA", "UNITED STATES", "US", "VEREINIGTE STAATEN" -> "US";
            case "GERMANY", "DEUTSCHLAND", "DE" -> "DE";
            case "FRANCE", "FRANKREICH", "FR" -> "FR";
            case "UK", "UNITED KINGDOM", "VEREINIGTES KÖNIGREICH", "GROSSBRITANNIEN", "GROẞBRITANNIEN", "GB" -> "GB";
            case "CHINA", "CN" -> "CN";
            case "JAPAN", "JP" -> "JP";
            case "TAIWAN", "TW" -> "TW";
            case "SOUTH KOREA", "KOREA", "SÜDKOREA", "KOREA, REPUBLIC OF", "REPUBLIK KOREA", "KR" -> "KR";
            case "CANADA", "KANADA", "CA" -> "CA";
            case "SWITZERLAND", "SCHWEIZ", "CH" -> "CH";
            case "NETHERLANDS", "NIEDERLANDE", "NL" -> "NL";
            case "ITALY", "ITALIEN", "IT" -> "IT";
            case "SPAIN", "SPANIEN", "ES" -> "ES";
            case "AUSTRALIA", "AUSTRALIEN", "AU" -> "AU";
            case "BRAZIL", "BRASILIEN", "BR" -> "BR";
            case "INDIA", "INDIEN", "IN" -> "IN";
            case "MEXICO", "MEXIKO", "MX" -> "MX";
            case "SWEDEN", "SCHWEDEN", "SE" -> "SE";
            case "DENMARK", "DÄNEMARK", "DK" -> "DK";
            case "NORWAY", "NORWEGEN", "NO" -> "NO";
            case "FINLAND", "FINNLAND", "FI" -> "FI";
            case "IRELAND", "IRLAND", "IE" -> "IE";
            case "BELGIUM", "BELGIEN", "BE" -> "BE";
            case "AUSTRIA", "ÖSTERREICH", "AT" -> "AT";
            case "POLAND", "POLEN", "PL" -> "PL";
            case "SINGAPORE", "SINGAPUR", "SG" -> "SG";
            case "HONG KONG", "HONGKONG", "HK" -> "HK";
            case "KUWAIT", "KW" -> "KW";
            case "CHILE", "CL" -> "CL";
            case "TURKEY", "TÜRKEI", "TURKEI", "TR" -> "TR";
            case "INDONESIA", "INDONESIEN", "ID" -> "ID";
            case "THAILAND", "TH" -> "TH";
            case "MALAYSIA", "MY" -> "MY";
            case "GREECE", "GRIECHENLAND", "GR" -> "GR";
            case "UNITED ARAB EMIRATES", "UAE", "VEREINIGTE ARABISCHE EMIRATE", "AE" -> "AE";
            case "SOUTH AFRICA", "SÜDAFRIKA", "SUDAFRIKA", "ZA" -> "ZA";
            default -> {
                // If it's already a 2-letter code, return it
                if (normalized.length() == 2 && normalized.matches("^[A-Z]{2}$")) {
                    yield normalized;
                }
                log.debug("Unmapped country name '{}' - no country code assigned", countryName);
                yield null;
            }
        };
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
     * Map Amundi sector names to GICS (Global Industry Classification Standard) sectors
     * @param sector The sector name from Amundi data
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
                 "insurance", "versicherungen", "financial services", "finanzdienstleistungen" ->
                "Financials";

            // Consumer Discretionary sector
            case "consumer discretionary", "consumer cyclical", "zyklische konsumgüter",
                 "konsumgüter zyklisch", "cyclical consumer goods", "retail", "einzelhandel",
                 "nicht-basiskonsumgüter", "nichtbasiskonsumgüter" ->
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
                 "chemicals", "chemie", "metals", "metalle", "mining", "bergbau", "werkstoffe" ->
                "Materials";

            // Real Estate sector
            case "real estate", "immobilien", "reits", "property" ->
                "Real Estate";

            // Utilities sector
            case "utilities", "versorgungsbetriebe", "versorger", "utility" ->
                "Utilities";

            // Communication Services sector
            case "communication services", "communications", "kommunikationsdienste", "kommunikation",
                 "telekommunikation", "telecommunication", "telecom", "media", "medien",
                 "kommunikationsdienstleistungen" ->
                "Communication Services";

            // Unknown/Other
            default -> {
                log.debug("Unmapped sector '{}' - using 'Unbekannt'", sector);
                yield "Unbekannt";
            }
        };
    }
}
