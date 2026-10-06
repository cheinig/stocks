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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Importer for iShares ETF holdings from web source.
 *
 * <p>iShares retired the old {@code {webUrl}/{webDataId}.ajax?tab=all&fileType=json} endpoint
 * (it now answers "File not found" with HTTP 404). The redesigned product pages load their
 * holdings table from BlackRock's product-data API instead:
 * {@code /varnish-api/uk-retail01-product-data/product-data/api/v2/get-product-data}.
 *
 * <p>That API needs a set of site parameters (locale, targetSite, userType, …) which are not
 * derivable from the product URL — the German site uses {@code de_DE}/{@code de-ishares-v2},
 * the UK site {@code en_GB}/{@code ishares-uk}. The product page embeds exactly those parameters
 * in the props of its holdings component, so they are read from the page at import time. No
 * per-ETF configuration beyond {@code ETF.webUrl} is required; the former {@code webDataId}
 * is obsolete.
 *
 * <p>Unlike the old row-oriented {@code aaData}, the API returns one parallel array per column
 * (issueName, isin, holdingPercent, sectorName, countryOfRisk, …).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ISharesWebImporter implements WebImporter {

    private static final String IMPORTER_NAME = "iShares Web";

    /** Holdings endpoint; the placeholders are filled from the product page's component context. */
    private static final String HOLDINGS_API_URL =
        "https://www.blackrock.com/varnish-api/uk-retail01-product-data/product-data/api/v2/get-product-data"
            + "?appType=PRODUCT_PAGE&appSubType=%s&targetSite=%s&locale=%s&userType=%s"
            + "&portfolioId=%s&portfolioType=%s&component=holdings&tab=all";

    /**
     * The holdings component of the product page carries its API parameters as a flat JSON
     * object, e.g. {@code "componentId":"holdings","context":{"productId":"251973",…}}.
     */
    private static final Pattern HOLDINGS_CONTEXT_PATTERN = Pattern.compile(
        "\"componentId\"\\s*:\\s*\"holdings\"\\s*,\\s*\"context\"\\s*:\\s*(\\{[^{}]*})");

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Fetch and parse holdings for one iShares product page.
     * @param webUrl the product page URL (stored in {@code ETF.webUrl}), e.g.
     *               {@code https://www.ishares.com/de/privatanleger/de/produkte/251973/ishares-…-fund}
     * @return List of allocation entries
     */
    @Override
    public List<AllocationEntry> fetchAndParse(String webUrl) {
        log.info("Starting iShares web import from URL: {}", webUrl);

        if (webUrl == null || webUrl.trim().isEmpty()) {
            throw new InvalidFileFormatException(IMPORTER_NAME, "No web URL provided");
        }

        try {
            ProductContext context = readProductContext(webUrl.trim());
            String apiUrl = String.format(HOLDINGS_API_URL,
                context.appSubType(), context.targetSite(), context.locale(),
                context.userType(), context.productId(), context.portfolioType());

            log.info("Fetching holdings JSON from: {}", apiUrl);
            String jsonResponse = restTemplate.getForObject(apiUrl, String.class);

            if (jsonResponse == null || jsonResponse.isEmpty()) {
                throw new InvalidFileFormatException(IMPORTER_NAME,
                    "Received empty response from holdings API: " + apiUrl);
            }

            log.info("Successfully fetched JSON data (size: {} bytes)", jsonResponse.length());

            return parseHoldings(jsonResponse);

        } catch (InvalidFileFormatException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to fetch data from URL: {}", webUrl, e);
            throw new InvalidFileFormatException(IMPORTER_NAME,
                "Failed to fetch data. Error: " + e.getMessage());
        }
    }

    /**
     * Load the product page and read the API parameters off its holdings component.
     */
    private ProductContext readProductContext(String webUrl) {
        log.debug("Reading holdings component context from product page: {}", webUrl);

        String page = restTemplate.getForObject(webUrl, String.class);
        if (page == null || page.isEmpty()) {
            throw new InvalidFileFormatException(IMPORTER_NAME,
                "Received empty response from product page: " + webUrl);
        }

        // The props are stored in an HTML attribute, so the JSON quotes arrive escaped
        Matcher matcher = HOLDINGS_CONTEXT_PATTERN.matcher(unescapeHtml(page));
        if (!matcher.find()) {
            throw new InvalidFileFormatException(IMPORTER_NAME,
                "Could not find the holdings component on the product page. "
                    + "Please check that the web URL points to an iShares product page: " + webUrl);
        }

        try {
            JsonNode context = objectMapper.readTree(matcher.group(1));
            ProductContext productContext = new ProductContext(
                requiredField(context, "productId"),
                requiredField(context, "portfolioType"),
                requiredField(context, "appSubType"),
                requiredField(context, "targetSite"),
                requiredField(context, "locale"),
                requiredField(context, "userType"));

            log.debug("Resolved product context: {}", productContext);
            return productContext;

        } catch (InvalidFileFormatException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidFileFormatException(IMPORTER_NAME,
                "Could not read the holdings parameters from the product page: " + e.getMessage());
        }
    }

    private String requiredField(JsonNode context, String field) {
        JsonNode value = context.get(field);
        if (value == null || value.asText().isEmpty()) {
            throw new InvalidFileFormatException(IMPORTER_NAME,
                "Holdings parameters on the product page are missing '" + field + "'");
        }
        return value.asText();
    }

    /**
     * Resolve the HTML entities that escape the JSON inside the {@code componentprops} attribute.
     * Only the handful that actually occur there; {@code &amp;} last so it cannot produce new ones.
     */
    private String unescapeHtml(String html) {
        return html
            .replace("&quot;", "\"")
            .replace("&#34;", "\"")
            .replace("&#39;", "'")
            .replace("&amp;", "&");
    }

    /**
     * Parse the holdings response of the product-data API. Every column is its own array under
     * {@code componentsByNameMap.holdings.containersByNameMap.all.dataPointsByNameMap}:
     * <pre>
     * "issueName":      {"value": ["SITC INTERNATIONAL HOLDINGS LTD", …]}
     * "isin":           {"value": ["KYG8187G1055", …]}          // null for cash/FX/futures rows
     * "holdingPercent": {"value": [2.82872, …]}
     * "sectorName":     {"value": ["Industrie", …]}
     * "countryOfRisk":  {"value": ["Hongkong", …]}
     * </pre>
     */
    private List<AllocationEntry> parseHoldings(String jsonResponse) {
        try {
            JsonNode dataPoints = objectMapper.readTree(jsonResponse)
                .path("componentsByNameMap").path("holdings")
                .path("containersByNameMap").path("all")
                .path("dataPointsByNameMap");

            if (dataPoints.isMissingNode() || !dataPoints.isObject()) {
                throw new InvalidFileFormatException(IMPORTER_NAME,
                    "Invalid JSON structure: no holdings data points found");
            }

            JsonNode names = column(dataPoints, "issueName");
            JsonNode percentages = column(dataPoints, "holdingPercent");
            JsonNode isins = column(dataPoints, "isin");
            JsonNode sectors = column(dataPoints, "sectorName");
            JsonNode countries = column(dataPoints, "countryOfRisk");

            if (names.size() != percentages.size()) {
                throw new InvalidFileFormatException(IMPORTER_NAME,
                    "Invalid JSON structure: got " + names.size() + " names but "
                        + percentages.size() + " weights");
            }

            List<AllocationEntry> entries = new ArrayList<>();

            for (int i = 0; i < names.size(); i++) {
                String name = text(names, i);
                if (name == null) {
                    log.warn("Skipping row {} - missing name", i + 1);
                    continue;
                }

                BigDecimal percentage = percentage(percentages, i);
                if (percentage == null || percentage.compareTo(new BigDecimal("0.000001")) < 0) {
                    // Cash, FX forwards and futures legs are listed with zero or negative weight
                    log.debug("Skipping row {} ({}) - weight is {}", i + 1, name, percentage);
                    continue;
                }

                String sector = normalizeSector(text(sectors, i));
                String mappedSector = mapSectorToGICS(sector);

                // If sector mapping returned null (e.g., for cash), use "Unbekannt" but don't track as unmapped
                String finalSector = mappedSector != null ? mappedSector : "Unbekannt";
                boolean shouldTrackAsUnmapped = mappedSector != null && "Unbekannt".equals(mappedSector);

                String isin = text(isins, i);
                String country = text(countries, i);

                AllocationEntry entry = AllocationEntry.builder()
                    .name(name)
                    .isin(isin != null ? isin : "")
                    .percentage(percentage)
                    .sector(finalSector)
                    .country(country != null ? mapCountryNameToCode(country) : null)
                    .originalSector(shouldTrackAsUnmapped && sector != null ? sector : null)
                    .build();

                entries.add(entry);
                log.debug("Parsed row {}: {} - {}%", i + 1, name, percentage);
            }

            if (entries.isEmpty()) {
                throw new InvalidFileFormatException(IMPORTER_NAME,
                    "No valid allocation entries found in JSON data");
            }

            log.info("Successfully parsed {} allocation entries from JSON", entries.size());
            return entries;

        } catch (InvalidFileFormatException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse JSON response", e);
            throw new InvalidFileFormatException(IMPORTER_NAME,
                "Failed to parse JSON data: " + e.getMessage());
        }
    }

    private JsonNode column(JsonNode dataPoints, String name) {
        JsonNode values = dataPoints.path(name).path("value");
        if (!values.isArray()) {
            throw new InvalidFileFormatException(IMPORTER_NAME,
                "Invalid JSON structure: holdings column '" + name + "' is missing");
        }
        return values;
    }

    /** Trimmed cell value, or null if the cell is absent, null or blank. */
    private String text(JsonNode column, int index) {
        JsonNode value = column.get(index);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText().trim();
        return text.isEmpty() ? null : text;
    }

    private BigDecimal percentage(JsonNode column, int index) {
        JsonNode value = column.get(index);
        if (value == null || value.isNull() || !value.isNumber()) {
            return null;
        }
        return value.decimalValue();
    }

    /**
     * iShares pads some sector names with a non-breaking space (e.g. "Zyklische Konsumgüter "),
     * which {@link String#strip()} does not remove.
     */
    private String normalizeSector(String sector) {
        if (sector == null) {
            return null;
        }
        String normalized = sector.replace(' ', ' ').strip();
        return normalized.isEmpty() ? null : normalized;
    }

    /**
     * Map iShares sector names to GICS (Global Industry Classification Standard) sectors
     * @param sector The sector name from iShares data
     * @return GICS-compliant sector name, "Unbekannt" if not mappable, or null for cash positions
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
            case "cash und/oder derivate", "cash and/or derivatives", "barmittel & derivate",
                 "cash and derivatives", "cash", "derivatives",
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
            case "irland", "ireland" -> "IE";
            case "belgien", "belgium" -> "BE";
            case "österreich", "austria" -> "AT";
            case "dänemark", "denmark" -> "DK";
            case "finnland", "finland" -> "FI";
            case "schweden", "sweden" -> "SE";
            case "norwegen", "norway" -> "NO";
            case "polen", "poland" -> "PL";
            case "portugal" -> "PT";
            case "griechenland", "greece" -> "GR";
            case "israel" -> "IL";
            case "neuseeland", "new zealand" -> "NZ";
            default -> null; // Return null for unmapped countries
        };
    }

    @Override
    public String getImporterName() {
        return IMPORTER_NAME;
    }

    /** The API parameters the product page hands to its holdings component. */
    private record ProductContext(
        String productId,
        String portfolioType,
        String appSubType,
        String targetSite,
        String locale,
        String userType) {
    }
}
