package com.stockstatus.service.importer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockstatus.dto.AllocationEntry;
import com.stockstatus.exception.InvalidFileFormatException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Importer for iShares ETF holdings from web source
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ISharesWebImporter implements WebImporter {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public List<AllocationEntry> fetchAndParse(String webUrl) {
        log.info("Starting iShares web import from URL: {}", webUrl);

        throw new UnsupportedOperationException(
            "iShares Web Importer requires webUrl and webDataId. " +
            "Use fetchAndParse(String webUrl, String webDataId) instead.");
    }

    /**
     * Fetch and parse holdings data using webUrl and webDataId
     * @param webUrl Base URL (e.g., https://www.ishares.com/de/privatanleger/de/produkte/251882/ishares-msci-world-ucits-etf-acc-fund)
     * @param webDataId Data ID for AJAX call (e.g., 1478358465952)
     * @return List of allocation entries
     */
    public List<AllocationEntry> fetchAndParse(String webUrl, String webDataId) {
        log.info("Starting iShares web import from URL: {} with dataId: {}", webUrl, webDataId);

        try {
            // Build JSON API URL: {webUrl}/{webDataId}.ajax?tab=all&fileType=json
            String cleanWebUrl = webUrl.endsWith("/") ? webUrl.substring(0, webUrl.length() - 1) : webUrl;
            String jsonUrl = cleanWebUrl + "/" + webDataId + ".ajax?tab=all&fileType=json";

            log.info("Fetching JSON data from: {}", jsonUrl);

            // Fetch the JSON data
            String jsonResponse = restTemplate.getForObject(jsonUrl, String.class);

            if (jsonResponse == null || jsonResponse.isEmpty()) {
                throw new InvalidFileFormatException("iShares Web",
                    "Received empty response from JSON endpoint: " + jsonUrl);
            }

            log.info("Successfully fetched JSON data (size: {} bytes)", jsonResponse.length());
            log.debug("JSON response preview: {}",
                jsonResponse.length() > 200 ? jsonResponse.substring(0, 200) + "..." : jsonResponse);

            // Remove BOM (Byte Order Mark) if present
            if (jsonResponse.startsWith("\uFEFF")) {
                jsonResponse = jsonResponse.substring(1);
                log.debug("Removed BOM character from JSON response");
            }

            // Parse JSON data
            return parseJsonResponse(jsonResponse);

        } catch (InvalidFileFormatException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to fetch data from URL: {} with dataId: {}", webUrl, webDataId, e);
            throw new InvalidFileFormatException("iShares Web",
                "Failed to fetch data. Error: " + e.getMessage());
        }
    }


    /**
     * Parse JSON response from iShares API
     * Expected structure:
     * {
     *   "aaData": [
     *     [
     *       "Ticker",           // 0
     *       "Name",             // 1
     *       "Sector",           // 2
     *       "Asset Class",      // 3
     *       {...},              // 4 - Market Value
     *       {"raw": 2.09},      // 5 - Weight (%)
     *       {...},              // 6 - Nominal Value
     *       {...},              // 7 - Nominal
     *       "ISIN",             // 8
     *       {...},              // 9 - Price
     *       "Location",         // 10
     *       "Exchange",         // 11
     *       "Currency"          // 12
     *     ]
     *   ]
     * }
     */
    private List<AllocationEntry> parseJsonResponse(String jsonResponse) {
        try {
            log.debug("Parsing JSON response");

            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode aaData = root.get("aaData");

            if (aaData == null || !aaData.isArray()) {
                throw new InvalidFileFormatException("iShares Web",
                    "Invalid JSON structure: 'aaData' array not found");
            }

            List<AllocationEntry> entries = new ArrayList<>();
            int rowNumber = 0;

            for (JsonNode row : aaData) {
                rowNumber++;

                if (!row.isArray() || row.size() < 13) {
                    log.warn("Skipping row {} - invalid structure or insufficient data (expected 13 elements, got {})",
                        rowNumber, row.size());
                    continue;
                }

                try {
                    // Extract data from the array
                    // Index 1: Name
                    String name = row.get(1).asText();

                    // Index 2: Sector - remove all whitespace including non-breaking spaces (\u00A0)
                    String sectorRaw = row.get(2).asText();
                    String sector = sectorRaw.strip();  // Java 11+ strip() removes all Unicode whitespace
                    if (sector.equals(sectorRaw)) {
                        // If strip() didn't change anything, try manual removal of non-breaking space
                        sector = sectorRaw.replace("\u00A0", "").trim();
                    }

                    // Index 5: Weight - get "raw" value from the object
                    JsonNode weightNode = row.get(5);
                    BigDecimal percentage = null;
                    if (weightNode.isObject() && weightNode.has("raw")) {
                        percentage = new BigDecimal(weightNode.get("raw").asText());
                    } else if (weightNode.isNumber()) {
                        percentage = new BigDecimal(weightNode.asDouble());
                    }

                    // Index 8: ISIN
                    String isin = row.get(8).asText();

                    // Index 10: Location (Country)
                    String location = row.get(10).asText();

                    // Validate required fields
                    if (name == null || name.trim().isEmpty()) {
                        log.warn("Skipping row {} - missing name", rowNumber);
                        continue;
                    }

                    if (percentage == null || percentage.compareTo(BigDecimal.ZERO) <= 0) {
                        log.warn("Skipping row {} - invalid percentage: {}", rowNumber, percentage);
                        continue;
                    }

                    // Map sector to GICS standard
                    String mappedSector = mapSectorToGICS(sector);

                    // If sector mapping returned null (e.g., for cash), use "Unbekannt" but don't track as unmapped
                    String finalSector = mappedSector != null ? mappedSector : "Unbekannt";
                    boolean shouldTrackAsUnmapped = mappedSector != null && "Unbekannt".equals(mappedSector);

                    // Build the allocation entry
                    AllocationEntry entry = AllocationEntry.builder()
                        .name(name.trim())
                        .isin(isin != null && !isin.trim().isEmpty() ? isin.trim() : "")
                        .percentage(percentage)
                        .sector(finalSector)
                        .country(location != null && !location.trim().isEmpty() ? mapCountryNameToCode(location.trim()) : null)
                        .originalSector(shouldTrackAsUnmapped && sector != null && !sector.isEmpty() ? sector : null)
                        .build();

                    entries.add(entry);
                    log.debug("Parsed row {}: {} - {}%", rowNumber, name, percentage);

                } catch (Exception e) {
                    log.warn("Error parsing row {}: {}", rowNumber, e.getMessage());
                    continue;
                }
            }

            if (entries.isEmpty()) {
                throw new InvalidFileFormatException("iShares Web",
                    "No valid allocation entries found in JSON data");
            }

            log.info("Successfully parsed {} allocation entries from JSON", entries.size());
            return entries;

        } catch (InvalidFileFormatException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse JSON response", e);
            throw new InvalidFileFormatException("iShares Web",
                "Failed to parse JSON data: " + e.getMessage());
        }
    }

    /**
     * Map iShares sector names to GICS (Global Industry Classification Standard) sectors
     * @param sector The sector name from iShares data
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

            // Cash positions and derivatives - not a real sector, map to null to skip tracking
            case "cash und/oder derivate", "cash and/or derivatives", "cash", "derivatives",
                 "bargeld", "liquidität", "liquidity" ->
                null;

            // Unknown/Other
            default -> {
                log.debug("Unmapped sector '{}' - using 'Unbekannt'", sector);
                yield "Unbekannt";
            }
        };
    }

    /**
     * Map country name to ISO 3166-1 alpha-2 code
     * This is a simplified mapping - can be extended as needed
     */
    private String mapCountryNameToCode(String countryName) {
        // Simple mapping for common countries
        return switch (countryName.toLowerCase()) {
            case "deutschland", "germany" -> "DE";
            case "vereinigte staaten", "united states", "usa" -> "US";
            case "vereinigtes königreich", "united kingdom", "uk" -> "GB";
            case "frankreich", "france" -> "FR";
            case "schweiz", "switzerland" -> "CH";
            case "niederlande", "netherlands" -> "NL";
            case "spanien", "spain" -> "ES";
            case "italien", "italy" -> "IT";
            case "japan" -> "JP";
            case "china" -> "CN";
            case "hongkong", "hong kong" -> "HK";
            case "australien", "australia" -> "AU";
            case "kanada", "canada" -> "CA";
            case "indien", "india" -> "IN";
            case "südkorea", "south korea" -> "KR";
            case "taiwan" -> "TW";
            case "brasilien", "brazil" -> "BR";
            case "mexiko", "mexico" -> "MX";
            case "singapur", "singapore" -> "SG";
            default -> null; // Return null for unmapped countries
        };
    }

    @Override
    public String getImporterName() {
        return "iShares Web";
    }
}
