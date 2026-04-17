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
 * Importer for VanEck ETF holdings from web source
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VanEckWebImporter implements WebImporter {

    private static final String BASE_API_URL = "https://www.vaneck.com/Main/HoldingsBlock/GetContent/?blockid=194733&pageid=233165&ticker=";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Fetch and parse VanEck holdings using ticker symbol
     * For VanEck Web, the parameter is the ticker symbol (e.g., "TDIV", "ESPO"), not a URL
     * @param tickerSymbol The VanEck ticker symbol (stored in ETF.tickerSymbol field)
     * @return List of allocation entries
     */
    @Override
    public List<AllocationEntry> fetchAndParse(String tickerSymbol) {
        log.info("Starting VanEck web import for ticker: {}", tickerSymbol);

        try {
            // Build API URL with ticker symbol
            String apiUrl = BASE_API_URL + tickerSymbol;

            log.info("Fetching JSON data from: {}", apiUrl);

            // Fetch the JSON data
            String jsonResponse = restTemplate.getForObject(apiUrl, String.class);

            if (jsonResponse == null || jsonResponse.isEmpty()) {
                throw new InvalidFileFormatException("VanEck Web",
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
            log.error("Failed to fetch data for ticker: {}", tickerSymbol, e);
            throw new InvalidFileFormatException("VanEck Web",
                "Failed to fetch data. Error: " + e.getMessage());
        }
    }

    /**
     * Parse JSON response from VanEck API
     * Expected structure:
     * {
     *   "data": {
     *     "Holdings": [
     *       {
     *         "ISIN": "KYG875721634",
     *         "HoldingName": "Tencent Holdings Ltd",
     *         "Weight": "8.40",
     *         "Sector": "Komunikation",
     *         "Country": "China"
     *       }
     *     ]
     *   }
     * }
     */
    private List<AllocationEntry> parseJsonResponse(String jsonResponse) {
        try {
            log.debug("Parsing JSON response");

            JsonNode root = objectMapper.readTree(jsonResponse);

            // Log the root structure for debugging
            log.debug("JSON root keys: {}", root.fieldNames());

            JsonNode data = root.get("data");

            if (data == null) {
                log.error("Invalid JSON structure. Root keys: {}", root.fieldNames());
                log.error("JSON preview (first 500 chars): {}",
                    jsonResponse.length() > 500 ? jsonResponse.substring(0, 500) : jsonResponse);
                throw new InvalidFileFormatException("VanEck Web",
                    "Invalid JSON structure: 'data' object not found. Check that the ticker symbol is correct.");
            }

            JsonNode holdings = data.get("Holdings");

            if (holdings == null || !holdings.isArray()) {
                throw new InvalidFileFormatException("VanEck Web",
                    "Invalid JSON structure: 'Holdings' array not found in data object");
            }

            List<AllocationEntry> entries = new ArrayList<>();
            int rowNumber = 0;

            for (JsonNode holding : holdings) {
                rowNumber++;

                try {
                    // Extract ISIN
                    String isin = holding.has("ISIN") ? holding.get("ISIN").asText() : "";

                    // Extract Name (HoldingName)
                    String name = holding.has("HoldingName") ? holding.get("HoldingName").asText() : "";

                    // Extract Weight as percentage
                    BigDecimal percentage = null;
                    if (holding.has("Weight")) {
                        String weightStr = holding.get("Weight").asText();
                        if (weightStr != null && !weightStr.trim().isEmpty()) {
                            try {
                                percentage = new BigDecimal(weightStr.trim());
                            } catch (NumberFormatException e) {
                                log.warn("Failed to parse weight '{}' for row {}", weightStr, rowNumber);
                            }
                        }
                    }

                    // Extract Country
                    String country = holding.has("Country") ? holding.get("Country").asText() : "";

                    // Extract Sector
                    String sector = holding.has("Sector") ? holding.get("Sector").asText() : "";

                    // Extract AssetClass to filter out cash positions
                    String assetClass = holding.has("AssetClass") ? holding.get("AssetClass").asText() : "";

                    // Skip cash and non-stock holdings
                    if (assetClass.equalsIgnoreCase("Cash Bal") ||
                        assetClass.equalsIgnoreCase("Barmittel") ||
                        name.trim().isEmpty() ||
                        isin.trim().isEmpty()) {
                        log.debug("Skipping row {} - cash/derivative position or missing data: {}", rowNumber, name);
                        continue;
                    }

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
                throw new InvalidFileFormatException("VanEck Web",
                    "No valid allocation entries found in JSON data");
            }

            log.info("Successfully parsed {} allocation entries from JSON", entries.size());
            return entries;

        } catch (InvalidFileFormatException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse JSON response", e);
            throw new InvalidFileFormatException("VanEck Web",
                "Failed to parse JSON data: " + e.getMessage());
        }
    }

    /**
     * Map VanEck sector names to GICS (Global Industry Classification Standard) sectors
     * Note: VanEck uses German sector names with some spelling variations
     * @param sector The sector name from VanEck data
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

            // Financials sector
            case "financials", "financial", "finanzen", "finanzwesen", "finanzdienstleister", "banks", "banken",
                 "insurance", "versicherungen", "financial services" ->
                "Financials";

            // Consumer Discretionary sector (including "Nicht-Basiskonsumgüter")
            case "consumer discretionary", "consumer cyclical", "zyklische konsumgüter", "verbrauchsgüter",
                 "konsumgüter zyklisch", "cyclical consumer goods", "retail", "einzelhandel",
                 "nicht-basiskonsumgüter", "nichtbasiskonsumgüter" ->
                "Consumer Discretionary";

            // Consumer Staples sector
            case "consumer staples", "consumer defensive", "basiskonsumgüter", "nicht-zyklische konsumgüter",
                 "nichtzyklische konsumgüter", "konsumgüter nicht-zyklisch", "non-cyclical consumer goods",
                 "food & beverage" ->
                "Consumer Staples";

            // Industrials sector
            case "industrials", "industrial", "industrie", "industriewerte", "industrieunternehmen", "machinery",
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
            case "utilities", "versorgungsbetriebe", "versorgungsunternehmen", "versorger", "utility" ->
                "Utilities";

            // Communication Services sector (VanEck uses "Komunikation" - note the spelling)
            case "communication services", "communications", "kommunikationsdienste", "kommunikation",
                 "telekommunikation", "telecommunication", "telecom", "media", "medien",
                 "komunikation" -> // VanEck's spelling variation
                "Communication Services";

            // Cash positions and derivatives - not a real sector, map to null to skip tracking
            case "cash und/oder derivate", "cash and/or derivatives", "cash", "derivatives",
                 "bargeld", "liquidität", "liquidity", "barmittel" ->
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
     * VanEck uses a mix of German and English country names
     */
    private String mapCountryNameToCode(String countryName) {
        // Simple mapping for common countries
        return switch (countryName.toLowerCase()) {
            case "deutschland", "germany" -> "DE";
            case "vereinigte staaten", "vereinigte staaten von amerika", "united states", "usa" -> "US";
            case "vereinigtes königreich", "united kingdom", "uk", "großbritannien" -> "GB";
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
            case "taiwan", "taiwan region" -> "TW";
            case "brasilien", "brazil" -> "BR";
            case "mexiko", "mexico" -> "MX";
            case "singapur", "singapore" -> "SG";
            case "belgien", "belgium" -> "BE";
            case "österreich", "austria" -> "AT";
            case "portugal" -> "PT";
            case "finnland", "finland" -> "FI";
            case "irland", "ireland" -> "IE";
            case "schweden", "sweden" -> "SE";
            case "polen", "poland" -> "PL";
            case "europe" -> null; // Generic "Europe" is not a country
            case "--" -> null; // VanEck uses "--" for unknown/empty countries
            default -> null; // Return null for unmapped countries
        };
    }

    @Override
    public String getImporterName() {
        return "VanEck Web";
    }
}
