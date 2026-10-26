package com.stockstatus.service.importer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.stockstatus.dto.AllocationEntry;
import com.stockstatus.exception.AllocationSumException;
import com.stockstatus.exception.InvalidFileFormatException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Importer for Vanguard (Europe) UCITS ETF holdings.
 *
 * The Vanguard Europe fund pages (global.vanguard.com) are server-side rendered and only show
 * the top holdings; the full list is loaded by the page's "Exportieren" button from the public
 * GraphQL gateway {@code /gpx/graphql}. The import works in two steps:
 * <ol>
 *   <li>{@code funds(isins: ...)} resolves the ISIN to Vanguard's internal port id
 *       (e.g. IE000VAHT5T0 -> E161).</li>
 *   <li>{@code borHoldings(portIds: ...)} returns the holdings in pages of up to 1500 items,
 *       continued via the opaque {@code lastItemKey} until it is null.</li>
 * </ol>
 * No per-ETF configuration is required — the ISIN from {@code ETF.isin} is enough.
 *
 * Special handling:
 * - Weights ({@code marketValuePercentage}) are already in percent
 * - Sectors are delivered as English GICS sector names and are used as-is
 * - Country is Bloomberg's ISO country, falling back to the ISIN prefix
 * - Cash, currency forwards and index futures have no ISIN and are grouped as "Sonstige";
 *   some of them carry negative weights, which is why they are netted rather than skipped
 * - A security may be listed more than once (e.g. separate lots); those rows are merged by ISIN
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VanguardWebImporter implements WebImporter {

    private static final String IMPORTER_NAME = "Vanguard Web";

    private static final String GRAPHQL_URL = "https://global.vanguard.com/gpx/graphql";

    /** The gateway rejects requests without a consumer id; "de7" is what the German site sends. */
    private static final String CONSUMER_ID = "de7";

    private static final String FUND_QUERY = """
        query FundLookupQuery($isins: [String!]) {
          funds(isins: $isins) {
            profile { portId fundFullName }
          }
        }
        """;

    /**
     * Same query the fund page uses for its holdings export, minus the {@code securityTypes}
     * filter: without it the response also contains cash and derivative positions, so that the
     * weights add up to 100%.
     */
    private static final String HOLDINGS_QUERY = """
        query FundsHoldingsQuery($portIds: [String!], $lastItemKey: String) {
          borHoldings(portIds: $portIds) {
            holdings(limit: 1500, lastItemKey: $lastItemKey) {
              items {
                issuerName
                securityLongDescription
                isin
                gicsSectorDescription
                marketValuePercentage
                bloombergIsoCountry
              }
              totalHoldings
              lastItemKey
            }
          }
        }
        """;

    /** Guards against a server that keeps returning a continuation key (~7400 holdings = 5 pages). */
    private static final int MAX_PAGES = 50;

    private static final BigDecimal MIN_SUM = new BigDecimal("99.0");
    private static final BigDecimal MAX_SUM = new BigDecimal("100.5");

    private static final String SONSTIGE_ISIN = "SONSTIGE00000";
    private static final String SONSTIGE_NAME = "Sonstige";
    private static final String UNKNOWN_SECTOR = "Unbekannt";

    private static final Set<String> GICS_SECTORS = Set.of(
        "Information Technology", "Health Care", "Financials", "Consumer Discretionary",
        "Consumer Staples", "Industrials", "Energy", "Materials", "Real Estate", "Utilities",
        "Communication Services");

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Fetch and parse Vanguard holdings using the ISIN of the share class.
     * @param isin The ETF ISIN (stored in ETF.isin field)
     * @return List of allocation entries
     */
    @Override
    public List<AllocationEntry> fetchAndParse(String isin) {
        log.info("Starting Vanguard web import for ISIN: {}", isin);

        if (isin == null || isin.trim().isEmpty()) {
            throw new InvalidFileFormatException(IMPORTER_NAME, "No ISIN provided");
        }
        String normalizedIsin = isin.trim().toUpperCase();

        try {
            String portId = lookupPortId(normalizedIsin);
            List<JsonNode> items = fetchHoldings(portId);
            return parseHoldings(items);

        } catch (InvalidFileFormatException | AllocationSumException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to fetch data for ISIN: {}", normalizedIsin, e);
            throw new InvalidFileFormatException(IMPORTER_NAME,
                "Failed to fetch or parse data. Error: " + e.getMessage());
        }
    }

    /**
     * Step 1: resolve the ISIN to Vanguard's internal port id.
     */
    private String lookupPortId(String isin) {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.putArray("isins").add(isin);

        JsonNode funds = query("FundLookupQuery", FUND_QUERY, variables).path("funds");
        if (!funds.isArray() || funds.isEmpty()) {
            throw new InvalidFileFormatException(IMPORTER_NAME,
                "ISIN " + isin + " was not found at Vanguard");
        }
        if (funds.size() > 1) {
            throw new InvalidFileFormatException(IMPORTER_NAME,
                "ISIN " + isin + " matches " + funds.size() + " Vanguard funds");
        }

        JsonNode profile = funds.get(0).path("profile");
        String portId = profile.path("portId").asText("");
        if (portId.isEmpty()) {
            throw new InvalidFileFormatException(IMPORTER_NAME,
                "Vanguard returned no port id for ISIN " + isin);
        }

        log.debug("Resolved ISIN {} to port id {} ({})", isin, portId, profile.path("fundFullName").asText());
        return portId;
    }

    /**
     * Step 2: read all holdings pages of the fund.
     */
    private List<JsonNode> fetchHoldings(String portId) {
        List<JsonNode> items = new ArrayList<>();
        Set<String> seenKeys = new HashSet<>();
        String lastItemKey = null;
        long totalHoldings = -1;

        for (int page = 1; page <= MAX_PAGES; page++) {
            ObjectNode variables = objectMapper.createObjectNode();
            variables.putArray("portIds").add(portId);
            variables.put("lastItemKey", lastItemKey);

            JsonNode borHoldings = query("FundsHoldingsQuery", HOLDINGS_QUERY, variables).path("borHoldings");
            // borHoldings is a list (one entry per requested port id)
            JsonNode holdings = (borHoldings.isArray() ? borHoldings.path(0) : borHoldings).path("holdings");
            if (holdings.isMissingNode() || holdings.isNull()) {
                throw new InvalidFileFormatException(IMPORTER_NAME,
                    "No holdings returned for port id " + portId);
            }

            holdings.path("items").forEach(items::add);
            totalHoldings = holdings.path("totalHoldings").asLong(-1);
            log.debug("Fetched holdings page {} for {}: {} items so far of {}",
                page, portId, items.size(), totalHoldings);

            JsonNode next = holdings.path("lastItemKey");
            if (next.isMissingNode() || next.isNull() || next.asText().isEmpty()) {
                if (totalHoldings >= 0 && totalHoldings != items.size()) {
                    throw new InvalidFileFormatException(IMPORTER_NAME, String.format(
                        "Holdings are incomplete: Vanguard reports %d, but %d were received",
                        totalHoldings, items.size()));
                }
                return items;
            }

            lastItemKey = next.asText();
            if (!seenKeys.add(lastItemKey)) {
                throw new InvalidFileFormatException(IMPORTER_NAME,
                    "Holdings pagination does not advance for port id " + portId);
            }
        }

        throw new InvalidFileFormatException(IMPORTER_NAME,
            "Holdings of port id " + portId + " exceed " + MAX_PAGES + " pages");
    }

    /**
     * Step 3: map the holdings to allocation entries.
     */
    private List<AllocationEntry> parseHoldings(List<JsonNode> items) {
        Map<String, AllocationEntry> entriesByIsin = new LinkedHashMap<>();
        BigDecimal totalPercentage = BigDecimal.ZERO;
        BigDecimal sonstigePercentage = BigDecimal.ZERO;

        for (JsonNode item : items) {
            JsonNode weight = item.path("marketValuePercentage");
            if (!weight.isNumber()) {
                log.warn("Skipping holding without weight: {}", item);
                continue;
            }
            BigDecimal percentage = weight.decimalValue();
            totalPercentage = totalPercentage.add(percentage);

            String isin = text(item, "isin");
            if (isin == null || !isin.matches("^[A-Z]{2}[A-Z0-9]{9}[0-9]$")) {
                // Cash, FX forwards, futures - netted, as they may be negative
                sonstigePercentage = sonstigePercentage.add(percentage);
                continue;
            }

            if (percentage.compareTo(BigDecimal.ZERO) <= 0) {
                log.debug("Skipping holding with non-positive weight: {}", isin);
                continue;
            }

            AllocationEntry existing = entriesByIsin.get(isin);
            if (existing != null) {
                existing.setPercentage(existing.getPercentage().add(percentage));
                continue;
            }

            String name = text(item, "issuerName");
            if (name == null) {
                name = text(item, "securityLongDescription");
            }
            if (name == null) {
                name = isin;
            }

            String sector = text(item, "gicsSectorDescription");
            boolean knownSector = sector != null && GICS_SECTORS.contains(sector);

            entriesByIsin.put(isin, AllocationEntry.builder()
                .isin(isin)
                .name(name)
                .percentage(percentage)
                .country(resolveCountry(text(item, "bloombergIsoCountry"), isin))
                .sector(knownSector ? sector : UNKNOWN_SECTOR)
                .originalSector(sector != null && !knownSector ? sector : null)
                .build());
        }

        List<AllocationEntry> entries = new ArrayList<>(entriesByIsin.values());

        if (sonstigePercentage.compareTo(BigDecimal.ZERO) > 0) {
            log.info("Adding 'Sonstige' entry with total percentage: {}%", sonstigePercentage);
            entries.add(AllocationEntry.builder()
                .isin(SONSTIGE_ISIN)
                .name(SONSTIGE_NAME)
                .percentage(sonstigePercentage)
                .country(null)
                .sector(null)
                .build());
        }

        if (entries.isEmpty()) {
            throw new InvalidFileFormatException(IMPORTER_NAME, "No valid holdings found");
        }

        if (totalPercentage.compareTo(MIN_SUM) < 0 || totalPercentage.compareTo(MAX_SUM) > 0) {
            throw new AllocationSumException(totalPercentage);
        }

        log.info("Successfully parsed {} allocation entries from {} Vanguard holdings, total: {}%",
            entries.size(), items.size(), totalPercentage);
        return entries;
    }

    /**
     * Run a GraphQL query and return its {@code data} node.
     */
    private JsonNode query(String operationName, String query, ObjectNode variables) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("operationName", operationName);
        body.set("variables", variables);
        body.put("query", query);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", "application/json");
        headers.set("x-consumer-id", CONSUMER_ID);
        headers.set("apollographql-client-name", "gpx");
        headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
            + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36");

        ResponseEntity<byte[]> response;
        try {
            response = restTemplate.exchange(GRAPHQL_URL, HttpMethod.POST,
                new HttpEntity<>(objectMapper.writeValueAsString(body), headers), byte[].class);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("Failed to build GraphQL request", e);
        }

        byte[] responseBody = response.getBody();
        if (responseBody == null || responseBody.length == 0) {
            throw new InvalidFileFormatException(IMPORTER_NAME,
                "Received empty response for " + operationName);
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(new String(responseBody, StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new InvalidFileFormatException(IMPORTER_NAME,
                "Failed to parse " + operationName + " response: " + e.getMessage());
        }

        JsonNode errors = root.path("errors");
        if (errors.isArray() && !errors.isEmpty()) {
            throw new InvalidFileFormatException(IMPORTER_NAME,
                operationName + " failed: " + errors.get(0).path("message").asText());
        }
        return root.path("data");
    }

    private String resolveCountry(String isoCountry, String isin) {
        if (isoCountry != null && isoCountry.matches("^[A-Z]{2}$")) {
            return isoCountry;
        }
        String prefix = isin.substring(0, 2);
        return prefix.matches("^[A-Z]{2}$") ? prefix : null;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        String text = value.asText().trim();
        return text.isEmpty() ? null : text;
    }

    @Override
    public String getImporterName() {
        return IMPORTER_NAME;
    }
}
