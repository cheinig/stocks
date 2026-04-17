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
 * Importer for XTrackers (DWS) ETF holdings from web source
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class XTrackersWebImporter implements WebImporter {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public List<AllocationEntry> fetchAndParse(String webUrl) {
        log.info("Starting XTrackers web import from URL: {}", webUrl);

        try {
            // Build API URL by transforming the base URL
            // Example: https://etf.dws.com/de-de/LU0292095535-euro-stoxx-quality-dividend-ucits-etf-1d
            // -> https://etf.dws.com/api/pdp/de-de/etf/LU0292095535-euro-stoxx-quality-dividend-ucits-etf-1d/holdings
            String apiUrl = buildApiUrl(webUrl);

            log.info("Fetching JSON data from: {}", apiUrl);

            // Fetch the JSON data
            String jsonResponse = restTemplate.getForObject(apiUrl, String.class);

            if (jsonResponse == null || jsonResponse.isEmpty()) {
                throw new InvalidFileFormatException("XTrackers Web",
                    "Received empty response from JSON endpoint: " + apiUrl);
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
            log.error("Failed to fetch data from URL: {}", webUrl, e);
            throw new InvalidFileFormatException("XTrackers Web",
                "Failed to fetch data. Error: " + e.getMessage());
        }
    }

    /**
     * Build API URL from base URL
     * Example transformation:
     * Input:  https://etf.dws.com/de-de/LU0292095535-euro-stoxx-quality-dividend-ucits-etf-1d
     * Output: https://etf.dws.com/api/pdp/de-de/etf/LU0292095535-euro-stoxx-quality-dividend-ucits-etf-1d/holdings
     */
    private String buildApiUrl(String baseUrl) {
        try {
            // Clean the URL
            String cleanUrl = baseUrl.trim();
            if (cleanUrl.endsWith("/")) {
                cleanUrl = cleanUrl.substring(0, cleanUrl.length() - 1);
            }

            log.info("Building API URL from base URL: {}", cleanUrl);

            // Extract domain and path parts
            // Expected format: https://etf.dws.com/de-de/LU0292095535-...
            int protocolEnd = cleanUrl.indexOf("://");
            if (protocolEnd == -1) {
                throw new IllegalArgumentException("Invalid URL format: missing protocol");
            }

            String protocol = cleanUrl.substring(0, protocolEnd + 3); // e.g., "https://"
            String remainingUrl = cleanUrl.substring(protocolEnd + 3); // e.g., "etf.dws.com/de-de/LU0292095535-..."

            int firstSlash = remainingUrl.indexOf('/');
            if (firstSlash == -1) {
                throw new IllegalArgumentException("Invalid URL format: missing path");
            }

            String domain = remainingUrl.substring(0, firstSlash); // e.g., "etf.dws.com"
            String path = remainingUrl.substring(firstSlash); // e.g., "/de-de/LU0292095535-..."

            // Build API URL: protocol + domain + /api/pdp + path + /holdings
            // Note: We need to add "/etf" before the ETF identifier if it's not already there
            String apiPath = path;

            // Check if path already contains "/etf/"
            if (!path.contains("/etf/")) {
                // Insert "/etf" after the locale (e.g., "/de-de" -> "/de-de/etf")
                // Path format is typically: /de-de/LU0292095535-...
                // We need: /de-de/etf/LU0292095535-...
                int secondSlash = path.indexOf('/', 1);
                if (secondSlash != -1) {
                    apiPath = path.substring(0, secondSlash) + "/etf" + path.substring(secondSlash);
                } else {
                    apiPath = path + "/etf";
                }
            }

            String apiUrl = protocol + domain + "/api/pdp" + apiPath + "/holdings";

            log.info("Transformed base URL '{}' to API URL '{}'", baseUrl, apiUrl);
            return apiUrl;

        } catch (Exception e) {
            log.error("Failed to build API URL from base URL: {}", baseUrl, e);
            throw new InvalidFileFormatException("XTrackers Web",
                "Invalid base URL format. Expected format: https://etf.dws.com/de-de/LU0292095535-etf-name. Error: " + e.getMessage());
        }
    }

    /**
     * Parse JSON response from XTrackers API
     * Expected structure:
     * {
     *   "tables": [
     *     {
     *       "values": [
     *         {
     *           "header": {"value": "ISIN"},
     *           "column_0": {"value": "Name"},
     *           "column_1": {"value": "Gewichtung %", "sortValue": 4.964099790},
     *           "column_2": {"value": "Marktwert"},
     *           "column_3": {"value": "Land"},
     *           "column_4": {"value": "Industrie"},
     *           "column_5": {"value": "Anlageklasse"}
     *         }
     *       ]
     *     }
     *   ]
     * }
     */
    private List<AllocationEntry> parseJsonResponse(String jsonResponse) {
        try {
            log.debug("Parsing JSON response");

            JsonNode root = objectMapper.readTree(jsonResponse);

            // Log the root structure for debugging
            log.debug("JSON root keys: {}", root.fieldNames());

            JsonNode tables = root.get("tables");

            if (tables == null || !tables.isArray() || tables.isEmpty()) {
                log.error("Invalid JSON structure. Root keys: {}", root.fieldNames());
                log.error("JSON preview (first 500 chars): {}",
                    jsonResponse.length() > 500 ? jsonResponse.substring(0, 500) : jsonResponse);
                throw new InvalidFileFormatException("XTrackers Web",
                    "Invalid JSON structure: 'tables' array not found or empty. Check that the URL is correct.");
            }

            // Get the first table
            JsonNode firstTable = tables.get(0);
            JsonNode values = firstTable.get("values");

            if (values == null || !values.isArray()) {
                throw new InvalidFileFormatException("XTrackers Web",
                    "Invalid JSON structure: 'values' array not found in table");
            }

            List<AllocationEntry> entries = new ArrayList<>();
            int rowNumber = 0;

            for (JsonNode row : values) {
                rowNumber++;

                try {
                    // Extract ISIN from header
                    JsonNode headerNode = row.get("header");
                    String isin = headerNode != null && headerNode.has("value")
                        ? headerNode.get("value").asText()
                        : "";

                    // Extract Name from column_0
                    JsonNode nameNode = row.get("column_0");
                    String name = nameNode != null && nameNode.has("value")
                        ? nameNode.get("value").asText()
                        : "";

                    // Extract Weight from column_1 (sortValue field)
                    JsonNode weightNode = row.get("column_1");
                    BigDecimal percentage = null;
                    if (weightNode != null && weightNode.has("sortValue")) {
                        percentage = new BigDecimal(weightNode.get("sortValue").asText());
                    }

                    // Extract Country from column_3
                    JsonNode countryNode = row.get("column_3");
                    String country = countryNode != null && countryNode.has("value")
                        ? countryNode.get("value").asText()
                        : "";

                    // Extract Sector from column_4
                    JsonNode sectorNode = row.get("column_4");
                    String sector = sectorNode != null && sectorNode.has("value")
                        ? sectorNode.get("value").asText()
                        : "";

                    // Validate required fields
                    if (name == null || name.trim().isEmpty()) {
                        log.warn("Skipping row {} - missing name", rowNumber);
                        continue;
                    }

                    if (percentage == null || percentage.compareTo(new BigDecimal("0.000001")) < 0) {
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
                        .country(country != null && !country.trim().isEmpty() ? mapCountryNameToCode(country.trim()) : null)
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
                throw new InvalidFileFormatException("XTrackers Web",
                    "No valid allocation entries found in JSON data");
            }

            log.info("Successfully parsed {} allocation entries from JSON", entries.size());
            return entries;

        } catch (InvalidFileFormatException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse JSON response", e);
            throw new InvalidFileFormatException("XTrackers Web",
                "Failed to parse JSON data: " + e.getMessage());
        }
    }

    /**
     * Map XTrackers sector names to GICS (Global Industry Classification Standard) sectors
     * @param sector The sector name from XTrackers data
     * @return GICS-compliant sector name or "Unbekannt" if not mappable
     */
    private String mapSectorToGICS(String sector) {
        if (sector == null || sector.trim().isEmpty() || sector.trim().equalsIgnoreCase("unbekannt")) {
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

            // Financials sector (including "Finanzdienstleister" from XTrackers)
            case "financials", "financial", "finanzen", "finanzwesen", "finanzdienstleister", "banks", "banken",
                 "insurance", "versicherungen", "financial services" ->
                "Financials";

            // Consumer Discretionary sector (including "Verbrauchsgüter" - discretionary consumer goods)
            case "consumer discretionary", "consumer cyclical", "zyklische konsumgüter", "verbrauchsgüter",
                 "konsumgüter zyklisch", "cyclical consumer goods", "retail", "einzelhandel" ->
                "Consumer Discretionary";

            // Consumer Staples sector (including "Basiskonsumgüter")
            case "consumer staples", "consumer defensive", "basiskonsumgüter", "nicht-zyklische konsumgüter",
                 "nichtzyklische konsumgüter", "konsumgüter nicht-zyklisch", "non-cyclical consumer goods",
                 "food & beverage" ->
                "Consumer Staples";

            // Industrials sector (including "Industrieunternehmen" from XTrackers)
            case "industrials", "industrial", "industrie", "industriewerte", "industrieunternehmen", "machinery",
                 "maschinen", "transportation", "transport" ->
                "Industrials";

            // Energy sector
            case "energy", "energie", "oil", "öl", "gas", "oil & gas", "petroleum" ->
                "Energy";

            // Materials sector (including "Grundstoffe" from XTrackers)
            case "materials", "basic materials", "rohstoffe", "grundstoffe", "materialien",
                 "chemicals", "chemie", "metals", "metalle", "mining", "bergbau" ->
                "Materials";

            // Real Estate sector (including "Immobilien" from XTrackers)
            case "real estate", "immobilien", "reits", "property" ->
                "Real Estate";

            // Utilities sector (including "Versorgungsunternehmen" from XTrackers)
            case "utilities", "versorgungsbetriebe", "versorgungsunternehmen", "versorger", "utility" ->
                "Utilities";

            // Communication Services sector (including "Telekommunikation" from XTrackers)
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
            case "vereinigte staaten", "vereinigte staaten von amerika", "united states", "usa" -> "US";
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
            case "belgien", "belgium" -> "BE";
            case "österreich", "austria" -> "AT";
            case "portugal" -> "PT";
            case "finnland", "finland" -> "FI";
            case "irland", "ireland" -> "IE";
            case "--" -> null; // XTrackers uses "--" for unknown/empty countries
            default -> null; // Return null for unmapped countries
        };
    }

    @Override
    public String getImporterName() {
        return "XTrackers Web";
    }
}
