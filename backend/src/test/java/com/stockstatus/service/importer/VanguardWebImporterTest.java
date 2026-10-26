package com.stockstatus.service.importer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockstatus.dto.AllocationEntry;
import com.stockstatus.exception.AllocationSumException;
import com.stockstatus.exception.InvalidFileFormatException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("VanguardWebImporter Unit Tests")
class VanguardWebImporterTest {

    private static final String GRAPHQL_URL = "https://global.vanguard.com/gpx/graphql";
    private static final String ISIN = "IE000VAHT5T0";

    private static final String FUND_JSON = """
        {"data":{"funds":[{"profile":{"portId":"E161",
          "fundFullName":"Vanguard FTSE Global All-Cap UCITS ETF USD Acc"}}]}}
        """;

    /** The continuation key is a JSON document serialized into a string, hence the escaping. */
    private static final String LAST_ITEM_KEY =
        "{\\\"skey\\\":0.01288,\\\"portIdKey\\\":\\\"fundBorHoldings-E161-2026-09-30\\\"}";

    /** First page, trimmed: two stocks plus the USD cash position (no ISIN). */
    private static final String PAGE_1_JSON = """
        {"data":{"borHoldings":[{"holdings":{
          "items":[
            {"issuerName":"NVIDIA Corp","securityLongDescription":"NVIDIA Corp","isin":"US67066G1040",
             "gicsSectorDescription":"Information Technology","marketValuePercentage":50.0,
             "bloombergIsoCountry":"US"},
            {"issuerName":"Apple Inc","securityLongDescription":"Apple Inc","isin":"US0378331005",
             "gicsSectorDescription":"Information Technology","marketValuePercentage":30.0,
             "bloombergIsoCountry":"US"},
            {"issuerName":null,"securityLongDescription":"US Dollar","isin":null,
             "gicsSectorDescription":null,"marketValuePercentage":5.0,"bloombergIsoCountry":"US"}
          ],
          "totalHoldings":8,
          "lastItemKey":"%s"}}]}}
        """.formatted(LAST_ITEM_KEY);

    /**
     * Second page: a second lot of Apple (to be merged), an FX forward with a negative weight
     * (netted into "Sonstige"), a holding without country and issuer name, one with an unknown
     * sector and a zero-weight right.
     */
    private static final String PAGE_2_JSON = """
        {"data":{"borHoldings":[{"holdings":{
          "items":[
            {"issuerName":"Apple Inc","securityLongDescription":"Apple Inc","isin":"US0378331005",
             "gicsSectorDescription":"Information Technology","marketValuePercentage":2.0,
             "bloombergIsoCountry":"US"},
            {"issuerName":null,"securityLongDescription":"VB1JOM4LUSCR3NAK","isin":null,
             "gicsSectorDescription":null,"marketValuePercentage":-1.0,"bloombergIsoCountry":null},
            {"issuerName":null,"securityLongDescription":"Shell PLC","isin":"GB00BP6MXD84",
             "gicsSectorDescription":"Energy","marketValuePercentage":10.0,"bloombergIsoCountry":null},
            {"issuerName":"Some Holding","securityLongDescription":"Some Holding","isin":"JP3000000001",
             "gicsSectorDescription":"Conglomerates","marketValuePercentage":4.0,
             "bloombergIsoCountry":"JP"},
            {"issuerName":"L&C Bio Co Ltd","securityLongDescription":"L&C BIO CO LTD-RIGHTS",
             "isin":"KRA2906501G7","gicsSectorDescription":"Health Care","marketValuePercentage":0,
             "bloombergIsoCountry":"KR"}
          ],
          "totalHoldings":8,
          "lastItemKey":null}}]}}
        """;

    private MockRestServiceServer server;
    private VanguardWebImporter importer;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        importer = new VanguardWebImporter(restTemplate, new ObjectMapper());
    }

    private void expectFundLookup(String body) {
        server.expect(requestTo(GRAPHQL_URL))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("x-consumer-id", "de7"))
            .andExpect(content().string(containsString("\"isins\":[\"" + ISIN + "\"]")))
            .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    }

    private void expectHoldingsPage(String lastItemKeyFragment, String body) {
        server.expect(requestTo(GRAPHQL_URL))
            .andExpect(content().string(containsString("\"portIds\":[\"E161\"]")))
            .andExpect(content().string(containsString(lastItemKeyFragment)))
            .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    }

    private static Map<String, AllocationEntry> byIsin(List<AllocationEntry> entries) {
        return entries.stream().collect(Collectors.toMap(AllocationEntry::getIsin, Function.identity()));
    }

    @Test
    @DisplayName("Should resolve the port id and read all holdings pages")
    void shouldImportAllPages() {
        expectFundLookup(FUND_JSON);
        expectHoldingsPage("\"lastItemKey\":null", PAGE_1_JSON);
        expectHoldingsPage("fundBorHoldings-E161-2026-09-30", PAGE_2_JSON);

        List<AllocationEntry> entries = importer.fetchAndParse(" ie000vaht5t0 ");

        server.verify();
        Map<String, AllocationEntry> entriesByIsin = byIsin(entries);
        assertThat(entriesByIsin).containsOnlyKeys(
            "US67066G1040", "US0378331005", "GB00BP6MXD84", "JP3000000001", "SONSTIGE00000");

        AllocationEntry nvidia = entriesByIsin.get("US67066G1040");
        assertThat(nvidia.getName()).isEqualTo("NVIDIA Corp");
        assertThat(nvidia.getPercentage()).isEqualByComparingTo("50.0");
        assertThat(nvidia.getCountry()).isEqualTo("US");
        assertThat(nvidia.getSector()).isEqualTo("Information Technology");
        assertThat(nvidia.getOriginalSector()).isNull();

        // Both Apple lots are merged into one entry
        assertThat(entriesByIsin.get("US0378331005").getPercentage()).isEqualByComparingTo("32.0");

        // Name falls back to the security description, country to the ISIN prefix
        AllocationEntry shell = entriesByIsin.get("GB00BP6MXD84");
        assertThat(shell.getName()).isEqualTo("Shell PLC");
        assertThat(shell.getCountry()).isEqualTo("GB");

        // Non-GICS sectors are reported as unmapped
        AllocationEntry unknown = entriesByIsin.get("JP3000000001");
        assertThat(unknown.getSector()).isEqualTo("Unbekannt");
        assertThat(unknown.getOriginalSector()).isEqualTo("Conglomerates");

        // Cash (5.0) and the negative FX forward (-1.0) are netted
        assertThat(entriesByIsin.get("SONSTIGE00000").getPercentage()).isEqualByComparingTo("4.0");

        assertThat(entries.stream().map(AllocationEntry::getPercentage).reduce(BigDecimal.ZERO, BigDecimal::add))
            .isEqualByComparingTo("100.0");
    }

    @Test
    @DisplayName("Should fail when the ISIN is unknown to Vanguard")
    void shouldFailForUnknownIsin() {
        expectFundLookup("{\"data\":{\"funds\":[]}}");

        assertThatThrownBy(() -> importer.fetchAndParse(ISIN))
            .isInstanceOf(InvalidFileFormatException.class)
            .hasMessageContaining("was not found");
    }

    @Test
    @DisplayName("Should fail when fewer holdings arrive than Vanguard reports")
    void shouldFailForIncompleteHoldings() {
        expectFundLookup(FUND_JSON);
        expectHoldingsPage("\"lastItemKey\":null",
            PAGE_1_JSON.replace("\"lastItemKey\":\"" + LAST_ITEM_KEY + "\"", "\"lastItemKey\":null"));

        assertThatThrownBy(() -> importer.fetchAndParse(ISIN))
            .isInstanceOf(InvalidFileFormatException.class)
            .hasMessageContaining("incomplete");
    }

    @Test
    @DisplayName("Should surface GraphQL errors")
    void shouldFailOnGraphQlError() {
        expectFundLookup("{\"errors\":[{\"message\":\"x-consumer-id must be provided\"}]}");

        assertThatThrownBy(() -> importer.fetchAndParse(ISIN))
            .isInstanceOf(InvalidFileFormatException.class)
            .hasMessageContaining("x-consumer-id must be provided");
    }

    @Test
    @DisplayName("Should reject holdings that do not add up to 100%")
    void shouldRejectWrongSum() {
        expectFundLookup(FUND_JSON);
        expectHoldingsPage("\"lastItemKey\":null", """
            {"data":{"borHoldings":[{"holdings":{
              "items":[{"issuerName":"NVIDIA Corp","isin":"US67066G1040",
                "gicsSectorDescription":"Information Technology","marketValuePercentage":50.0,
                "bloombergIsoCountry":"US"}],
              "totalHoldings":1,"lastItemKey":null}}]}}
            """);

        assertThatThrownBy(() -> importer.fetchAndParse(ISIN))
            .isInstanceOf(AllocationSumException.class);
    }

    @Test
    @DisplayName("Should reject a missing ISIN")
    void shouldRejectMissingIsin() {
        assertThatThrownBy(() -> importer.fetchAndParse("  "))
            .isInstanceOf(InvalidFileFormatException.class)
            .hasMessageContaining("No ISIN provided");
    }
}
