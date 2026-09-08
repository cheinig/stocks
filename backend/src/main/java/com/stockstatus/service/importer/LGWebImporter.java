package com.stockstatus.service.importer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVReader;
import com.stockstatus.dto.AllocationEntry;
import com.stockstatus.exception.AllocationSumException;
import com.stockstatus.exception.InvalidFileFormatException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Importer for L&G (Legal &amp; General / LGIM) ETF holdings from their fund centre.
 *
 * The holdings file is a PCF (portfolio composition file) that is served under an opaque,
 * per-share-class document UUID. That UUID is not derivable from the ISIN, so it is resolved
 * at import time in three steps:
 * <ol>
 *   <li>{@code /srp/api/fund-centre/48} returns the whole ETF catalogue and maps the ISIN
 *       to the numeric fund id and share class id.</li>
 *   <li>{@code /srp/api/part?id=12610&fund_id=...} is the CMS fragment the fund page uses to
 *       render its download links; it contains the holdings URL for every share class of that
 *       fund, keyed by share class id.</li>
 *   <li>The resolved URL returns the PCF as CSV.</li>
 * </ol>
 * No per-ETF configuration is required — the ISIN from {@code ETF.isin} is enough.
 *
 * Special handling:
 * - Constituent weights are fractions (0..1) and are converted to percent
 * - Country code is extracted from the first 2 characters of the ISIN
 * - Sector is not part of the PCF and is left empty
 * - Entries without ISIN are grouped as "Sonstige"
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LGWebImporter implements WebImporter {

    private static final String IMPORTER_NAME = "L&G Web";

    /** Fund centre 48 = "ETF" range, audience 150 = German private investors. */
    private static final String CATALOG_URL =
        "https://fundcentres.landg.com/srp/api/fund-centre/48?audience=150&language=3";

    /** CMS part 12610 renders the holdings download links of all share classes of one fund. */
    private static final String HOLDINGS_PART_URL =
        "https://fundcentres.landg.com/srp/api/part?id=12610&audience=150&fund_id=%s";

    private static final String ISIN_FIELD = "shareclassISIN";
    private static final String HEADER_PREFIX = "Security Description";
    private static final String TRADING_ID_PREFIX = "ETF Trading ID";
    private static final String RECORD_COUNT_PREFIX = "Record Count";

    /**
     * The constituent weights deliberately do not add up to 100%: the remainder is the cash
     * component ("1 minus the sum of constituent weights") and is not listed as a row. The lower
     * bound is therefore looser than in the file importers; correctness is instead asserted via
     * the "ETF Trading ID" and "Record Count" fields of the PCF.
     */
    private static final BigDecimal MIN_SUM = new BigDecimal("98.0");
    private static final BigDecimal MAX_SUM = new BigDecimal("100.5");
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private static final String SONSTIGE_ISIN = "SONSTIGE00000";
    private static final String SONSTIGE_NAME = "Sonstige";

    /** Matches one {@code { ... }} document entry inside a share class block. */
    private static final Pattern DOCUMENT_PATTERN = Pattern.compile("\\{([^{}]*)}");
    private static final Pattern URL_PATTERN = Pattern.compile("url\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern CSV_TYPE_PATTERN = Pattern.compile("type\\s*:\\s*\"csv\"");

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Fetch and parse L&amp;G holdings using the ISIN of the share class.
     * @param isin The ETF ISIN (stored in ETF.isin field)
     * @return List of allocation entries
     */
    @Override
    public List<AllocationEntry> fetchAndParse(String isin) {
        log.info("Starting L&G web import for ISIN: {}", isin);

        if (isin == null || isin.trim().isEmpty()) {
            throw new InvalidFileFormatException(IMPORTER_NAME, "No ISIN provided");
        }
        String normalizedIsin = isin.trim().toUpperCase();

        try {
            ShareClassRef shareClass = lookupShareClass(normalizedIsin);
            String holdingsUrl = resolveHoldingsUrl(shareClass);

            log.info("Fetching holdings CSV from: {}", holdingsUrl);
            String csv = fetchAsUtf8(holdingsUrl);

            return parseCsv(csv, normalizedIsin);

        } catch (InvalidFileFormatException | AllocationSumException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to fetch data for ISIN: {}", normalizedIsin, e);
            throw new InvalidFileFormatException(IMPORTER_NAME,
                "Failed to fetch or parse data. Error: " + e.getMessage());
        }
    }

    /**
     * Step 1: resolve the ISIN to the numeric fund id and share class id via the ETF catalogue.
     */
    private ShareClassRef lookupShareClass(String isin) {
        log.debug("Looking up fund and share class ids from: {}", CATALOG_URL);

        String json = fetchAsUtf8(CATALOG_URL);
        JsonNode root;
        try {
            root = objectMapper.readTree(json);
        } catch (Exception e) {
            throw new InvalidFileFormatException(IMPORTER_NAME,
                "Failed to parse ETF catalogue: " + e.getMessage());
        }

        // The catalogue stores share class values as a positional array; the field order is
        // described by metadata.share_class_fields[].code_name.
        JsonNode fields = root.path("metadata").path("share_class_fields");
        int isinIndex = -1;
        for (int i = 0; i < fields.size(); i++) {
            if (ISIN_FIELD.equals(fields.get(i).path("code_name").asText())) {
                isinIndex = i;
                break;
            }
        }
        if (isinIndex < 0) {
            throw new InvalidFileFormatException(IMPORTER_NAME,
                "ETF catalogue does not contain a '" + ISIN_FIELD + "' field - the API format changed");
        }

        JsonNode funds = root.path("funds");
        if (!funds.isArray() || funds.isEmpty()) {
            throw new InvalidFileFormatException(IMPORTER_NAME, "ETF catalogue contains no funds");
        }

        for (JsonNode fund : funds) {
            for (JsonNode shareClass : fund.path("share_classes")) {
                JsonNode data = shareClass.path("data");
                if (isinIndex >= data.size()) {
                    continue;
                }
                if (isin.equalsIgnoreCase(data.get(isinIndex).asText())) {
                    ShareClassRef ref = new ShareClassRef(
                        fund.path("id").asLong(), shareClass.path("id").asLong());
                    log.debug("Resolved ISIN {} to fund {} / share class {}",
                        isin, ref.fundId(), ref.shareClassId());
                    return ref;
                }
            }
        }

        throw new InvalidFileFormatException(IMPORTER_NAME,
            "ISIN " + isin + " was not found in the L&G ETF catalogue");
    }

    /**
     * Step 2: read the holdings download URL for the share class out of the CMS fragment.
     * The fragment embeds a JavaScript-object-ish (not strictly JSON) block per share class,
     * which may list several documents; only the one with {@code type: "csv"} is the PCF.
     */
    private String resolveHoldingsUrl(ShareClassRef shareClass) {
        String partUrl = String.format(HOLDINGS_PART_URL, shareClass.fundId());
        log.debug("Resolving holdings URL from: {}", partUrl);

        String body = fetchAsUtf8(partUrl);

        Matcher block = Pattern
            .compile("\"" + shareClass.shareClassId() + "\"\\s*:\\s*\\[(.*?)]", Pattern.DOTALL)
            .matcher(body);
        if (!block.find()) {
            throw new InvalidFileFormatException(IMPORTER_NAME,
                "No holdings documents listed for share class " + shareClass.shareClassId());
        }

        Matcher documents = DOCUMENT_PATTERN.matcher(block.group(1));
        while (documents.find()) {
            String document = documents.group(1);
            if (!CSV_TYPE_PATTERN.matcher(document).find()) {
                // e.g. the "collateral constituents" API link of synthetic ETFs
                continue;
            }
            Matcher url = URL_PATTERN.matcher(document);
            if (url.find()) {
                return url.group(1);
            }
        }

        throw new InvalidFileFormatException(IMPORTER_NAME,
            "No CSV holdings document found for share class " + shareClass.shareClassId());
    }

    /**
     * Step 3: parse the PCF.
     * Expected format:
     * <pre>
     * sep=,
     * Basket NameL&amp;G Global Quality Dividends UCITS ETF - Dist
     * ETF Trading IDIE0005AJA0P1
     * Basket Trade Date2026-09-07
     * Security Description,ISIN,Trading Currency,Constituent Weight (Base)
     * JYSKE BANK A/S DKK 10.0,DK0010307958,DKK,0.001361471901
     * ...
     *
     * Cash Component is a balancing number.  Formula = "1 minus the sum of constituent weights"
     *
     * "Basket Client Disclaimer..."
     * Record Count955
     * </pre>
     * Note that the leading metadata lines carry their label and value concatenated without a
     * separator, and that the trailer after the data rows has a varying number of fields.
     */
    private List<AllocationEntry> parseCsv(String csv, String expectedIsin) {
        List<String> lines = csv.lines().toList();

        String tradingId = findValue(lines, TRADING_ID_PREFIX);
        if (tradingId == null) {
            throw new InvalidFileFormatException(IMPORTER_NAME,
                "Not an L&G holdings file - no '" + TRADING_ID_PREFIX + "' header found");
        }
        if (!expectedIsin.equalsIgnoreCase(tradingId)) {
            // The document UUID is resolved per share class, so a mismatch means the upstream
            // mapping changed. Importing another fund's holdings would silently corrupt the ETF.
            throw new InvalidFileFormatException(IMPORTER_NAME, String.format(
                "Holdings file belongs to ISIN %s but %s was requested", tradingId, expectedIsin));
        }

        int headerIndex = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).startsWith(HEADER_PREFIX)) {
                headerIndex = i;
                break;
            }
        }
        if (headerIndex < 0) {
            throw new InvalidFileFormatException(IMPORTER_NAME,
                "Holdings file has no '" + HEADER_PREFIX + "' header row");
        }

        List<AllocationEntry> entries = new ArrayList<>();
        BigDecimal totalPercentage = BigDecimal.ZERO;
        BigDecimal sonstigePercentage = BigDecimal.ZERO;
        int rowCount = 0;

        String dataSection = String.join("\n", lines.subList(headerIndex + 1, lines.size()));
        List<String[]> rows;
        try (CSVReader reader = new CSVReader(new StringReader(dataSection))) {
            rows = reader.readAll();
        } catch (Exception e) {
            throw new InvalidFileFormatException(IMPORTER_NAME,
                "Failed to read holdings rows: " + e.getMessage());
        }

        for (String[] row : rows) {
            // The trailer (blank lines, cash component note, disclaimer, record count) does not
            // have the 4 columns of a holding.
            if (row.length != 4) {
                continue;
            }

            String name = row[0].trim();
            String isin = row[1].trim();
            String weight = row[3].trim();

            if (name.isEmpty() || weight.isEmpty()) {
                log.warn("Skipping holdings row without name or weight: {}", name);
                continue;
            }

            BigDecimal percentage;
            try {
                // Weights are fractions of 1, not percent
                percentage = new BigDecimal(weight).multiply(HUNDRED);
            } catch (NumberFormatException e) {
                throw new InvalidFileFormatException(IMPORTER_NAME,
                    String.format("Invalid weight format for '%s': %s", name, weight));
            }

            rowCount++;
            totalPercentage = totalPercentage.add(percentage);

            if (percentage.compareTo(BigDecimal.ZERO) <= 0) {
                log.debug("Skipping holding with non-positive weight: {}", name);
                continue;
            }

            if (isin.isEmpty()) {
                sonstigePercentage = sonstigePercentage.add(percentage);
                continue;
            }

            entries.add(AllocationEntry.builder()
                .isin(isin)
                .name(name)
                .percentage(percentage)
                .country(extractCountryFromIsin(isin))
                .sector(null)
                .build());
        }

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
            throw new InvalidFileFormatException(IMPORTER_NAME, "No valid data rows found");
        }

        // The PCF states its own row count - use it to detect a truncated download.
        String recordCount = findValue(lines, RECORD_COUNT_PREFIX);
        if (recordCount != null) {
            try {
                int expected = Integer.parseInt(recordCount.trim());
                if (expected != rowCount) {
                    throw new InvalidFileFormatException(IMPORTER_NAME, String.format(
                        "Holdings file is incomplete: %s says %d, but %d rows were parsed",
                        RECORD_COUNT_PREFIX, expected, rowCount));
                }
            } catch (NumberFormatException e) {
                log.warn("Could not read '{}' value '{}' - skipping row count check",
                    RECORD_COUNT_PREFIX, recordCount);
            }
        }

        if (totalPercentage.compareTo(MIN_SUM) < 0 || totalPercentage.compareTo(MAX_SUM) > 0) {
            throw new AllocationSumException(totalPercentage);
        }

        log.info("Successfully parsed {} allocation entries from L&G holdings file, total: {}%",
            entries.size(), totalPercentage);
        return entries;
    }

    /**
     * Read one of the leading metadata lines, whose label and value are concatenated without
     * a separator (e.g. {@code ETF Trading IDIE0005AJA0P1}).
     */
    private String findValue(List<String> lines, String prefix) {
        for (String line : lines) {
            if (line.startsWith(prefix)) {
                return line.substring(prefix.length()).trim();
            }
        }
        return null;
    }

    private String extractCountryFromIsin(String isin) {
        if (isin == null || isin.length() < 2) {
            return null;
        }
        String countryCode = isin.substring(0, 2).toUpperCase();
        return countryCode.matches("^[A-Z]{2}$") ? countryCode : null;
    }

    /**
     * Fetch a URL as UTF-8. The holdings CSV is served as {@code text/csv} without a charset,
     * for which RestTemplate would otherwise fall back to ISO-8859-1 and mangle security names.
     */
    private String fetchAsUtf8(String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
            + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36");
        headers.set("Accept", "*/*");

        ResponseEntity<byte[]> response =
            restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), byte[].class);

        byte[] body = response.getBody();
        if (body == null || body.length == 0) {
            // The fund centre sits behind AWS WAF, which answers a rate-limit challenge with an
            // empty 202 body instead of an error status.
            throw new InvalidFileFormatException(IMPORTER_NAME,
                "Received empty response from: " + url);
        }

        String content = new String(body, StandardCharsets.UTF_8);
        return content.startsWith("﻿") ? content.substring(1) : content;
    }

    @Override
    public String getImporterName() {
        return IMPORTER_NAME;
    }

    /** Identifies one L&G share class within the fund centre API. */
    private record ShareClassRef(long fundId, long shareClassId) {
    }
}
