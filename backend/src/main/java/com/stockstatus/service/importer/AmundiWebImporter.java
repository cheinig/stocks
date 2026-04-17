package com.stockstatus.service.importer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockstatus.dto.AllocationEntry;
import com.stockstatus.exception.InvalidFileFormatException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Importer for Amundi ETF holdings from web source
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AmundiWebImporter implements WebImporter {

    private static final String API_URL = "https://www.amundietf.de/mapi/ProductAPI/getProductsData";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Fetch and parse Amundi holdings using ISIN
     * For Amundi Web, the parameter is the ISIN (e.g., "IE000CNSFAR2")
     * @param isin The ETF ISIN (stored in ETF.isin field)
     * @return List of allocation entries
     */
    @Override
    public List<AllocationEntry> fetchAndParse(String isin) {
        log.info("Starting Amundi web import for ISIN: {}", isin);

        try {
            // Build request body
            Map<String, Object> requestBody = buildRequestBody(isin);

            // Convert request body to JSON string
            String jsonRequestBody = objectMapper.writeValueAsString(requestBody);

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> request = new HttpEntity<>(jsonRequestBody, headers);

            log.info("Fetching JSON data from: {}", API_URL);
            log.debug("Request body JSON: {}", jsonRequestBody);

            // Fetch the JSON data
            String jsonResponse = restTemplate.postForObject(API_URL, request, String.class);

            if (jsonResponse == null || jsonResponse.isEmpty()) {
                throw new InvalidFileFormatException("Amundi Web",
                    "Received empty response from API endpoint: " + API_URL);
            }

            log.info("Successfully fetched JSON data (size: {} bytes)", jsonResponse.length());
            log.debug("JSON response preview: {}",
                jsonResponse.length() > 200 ? jsonResponse.substring(0, 200) + "..." : jsonResponse);

            // Parse JSON data
            return parseJsonResponse(jsonResponse);

        } catch (InvalidFileFormatException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to fetch data for ISIN: {}", isin, e);
            throw new InvalidFileFormatException("Amundi Web",
                "Failed to fetch data. Error: " + e.getMessage());
        }
    }

    /**
     * Build the request body for Amundi API
     */
    private Map<String, Object> buildRequestBody(String isin) {
        Map<String, Object> requestBody = new HashMap<>();

        // Set productIds array
        List<String> productIds = new ArrayList<>();
        productIds.add(isin);
        requestBody.put("productIds", productIds);

        // Set composition fields
        Map<String, Object> composition = new HashMap<>();
        List<String> compositionFields = List.of(
            "date", "type", "bbg", "isin", "name", "weight",
            "quantity", "currency", "sector", "country", "countryOfRisk"
        );
        composition.put("compositionFields", compositionFields);
        requestBody.put("composition", composition);

        return requestBody;
    }

    /**
     * Parse JSON response from Amundi API
     * Expected structure:
     * {
     *   "products": [
     *     {
     *       "productId": "IE000CNSFAR2",
     *       "composition": {
     *         "totalNumberOfInstruments": 1,
     *         "compositionData": [
     *           {
     *             "compositionCharacteristics": {
     *               "date": "2026-01-06",
     *               "quantity": 4585476.0,
     *               "bbg": "NVDA UW",
     *               "name": "NVIDIA CORP",
     *               "weight": 0.053829979699368063,
     *               "currency": "USD",
     *               "type": "EQUITY_ORDINARY",
     *               "sector": "Information Technology",
     *               "isin": "US67066G1040",
     *               "countryOfRisk": "United States"
     *             },
     *             "weight": 0.053829979699368063
     *           }
     *         ]
     *       }
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

            JsonNode products = root.get("products");

            if (products == null || !products.isArray() || products.isEmpty()) {
                log.error("Invalid JSON structure. Root keys: {}", root.fieldNames());
                log.error("JSON preview (first 500 chars): {}",
                    jsonResponse.length() > 500 ? jsonResponse.substring(0, 500) : jsonResponse);
                throw new InvalidFileFormatException("Amundi Web",
                    "Invalid JSON structure: 'products' array not found or empty. Check that the ISIN is correct.");
            }

            JsonNode product = products.get(0);
            JsonNode composition = product.get("composition");

            if (composition == null) {
                throw new InvalidFileFormatException("Amundi Web",
                    "Invalid JSON structure: 'composition' object not found in product");
            }

            JsonNode compositionData = composition.get("compositionData");

            if (compositionData == null || !compositionData.isArray()) {
                throw new InvalidFileFormatException("Amundi Web",
                    "Invalid JSON structure: 'compositionData' array not found in composition object");
            }

            List<AllocationEntry> entries = new ArrayList<>();
            int rowNumber = 0;

            for (JsonNode holding : compositionData) {
                rowNumber++;

                try {
                    JsonNode characteristics = holding.get("compositionCharacteristics");

                    if (characteristics == null) {
                        log.warn("Skipping row {} - missing compositionCharacteristics", rowNumber);
                        continue;
                    }

                    // Extract ISIN
                    String isin = characteristics.has("isin") ? characteristics.get("isin").asText() : "";

                    // Extract Name
                    String name = characteristics.has("name") ? characteristics.get("name").asText() : "";

                    // Extract Weight as percentage (Amundi provides it as decimal, e.g., 0.053829979699368063 = 5.38%)
                    BigDecimal percentage = null;
                    if (characteristics.has("weight")) {
                        double weight = characteristics.get("weight").asDouble();
                        // Convert decimal to percentage (multiply by 100)
                        percentage = BigDecimal.valueOf(weight * 100);
                    }

                    // Extract Country (using countryOfRisk)
                    String country = characteristics.has("countryOfRisk") ? characteristics.get("countryOfRisk").asText() : "";

                    // Extract Sector
                    String sector = characteristics.has("sector") ? characteristics.get("sector").asText() : "";

                    // Extract Type to filter out non-equity positions
                    String type = characteristics.has("type") ? characteristics.get("type").asText() : "";

                    // Skip non-equity positions (cash, derivatives, etc.)
                    if (!type.contains("EQUITY") || name.trim().isEmpty()) {
                        log.debug("Skipping row {} - non-equity position or missing data: {} (type: {})", rowNumber, name, type);
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
                throw new InvalidFileFormatException("Amundi Web",
                    "No valid allocation entries found in JSON data");
            }

            log.info("Successfully parsed {} allocation entries from JSON", entries.size());
            return entries;

        } catch (InvalidFileFormatException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse JSON response", e);
            throw new InvalidFileFormatException("Amundi Web",
                "Failed to parse JSON data: " + e.getMessage());
        }
    }

    /**
     * Map Amundi sector names to GICS (Global Industry Classification Standard) sectors
     * @param sector The sector name from Amundi data
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

            // Consumer Discretionary sector
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

            // Communication Services sector
            case "communication services", "communications", "kommunikationsdienste", "kommunikation",
                 "telekommunikation", "telecommunication", "telecom", "media", "medien" ->
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
     * Amundi uses English country names
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
            case "südkorea", "south korea", "korea" -> "KR";
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
            case "dänemark", "denmark" -> "DK";
            case "norwegen", "norway" -> "NO";
            case "russland", "russia" -> "RU";
            case "südafrika", "south africa" -> "ZA";
            case "israel" -> "IL";
            case "türkei", "turkey" -> "TR";
            case "europe" -> null; // Generic "Europe" is not a country
            case "--" -> null; // Unknown/empty countries
            default -> null; // Return null for unmapped countries
        };
    }

    @Override
    public String getImporterName() {
        return "Amundi Web";
    }
}
