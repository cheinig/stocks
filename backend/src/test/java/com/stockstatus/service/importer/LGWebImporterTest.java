package com.stockstatus.service.importer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockstatus.dto.AllocationEntry;
import com.stockstatus.exception.AllocationSumException;
import com.stockstatus.exception.InvalidFileFormatException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("LGWebImporter Unit Tests")
class LGWebImporterTest {

    private static final String CATALOG_URL =
        "https://fundcentres.landg.com/srp/api/fund-centre/48?audience=150&language=3";
    private static final String PART_URL =
        "https://fundcentres.landg.com/srp/api/part?id=12610&audience=150&fund_id=6952";
    private static final String CSV_URL =
        "https://fundcentres.landg.com/srp/documents-id/0c10d7c1/Fundholdings.csv";
    private static final String ACC_CSV_URL =
        "https://fundcentres.lgim.com/srp/documents-id/830184ff/Fundholdings.pdf";

    private static final String ISIN_DIST = "IE0005AJA0P1";
    private static final String ISIN_ACC = "IE000MRIQ479";

    /** Trimmed to the fields the importer actually reads. */
    private static final String CATALOG_JSON = """
        {
          "metadata": {
            "share_class_fields": [
              {"code_name": "shareclassPageURL"},
              {"code_name": "ter"},
              {"code_name": "shareclassISIN"},
              {"code_name": "shareclassCurrency"}
            ]
          },
          "funds": [
            {
              "id": 95,
              "share_classes": [
                {"id": 890, "data": ["/etf/russell/", "0.30", "IE00B3CNHJ55", "USD"]}
              ]
            },
            {
              "id": 6952,
              "share_classes": [
                {"id": 14653, "data": ["/etf/gqd/acc/", "0.28", "IE000MRIQ479", "USD"]},
                {"id": 15177, "data": ["/etf/gqd/dist/", "0.28", "IE0005AJA0P1", "USD"]}
              ]
            }
          ]
        }
        """;

    /**
     * The CMS fragment is JavaScript-object-ish, not strict JSON, and lists several documents
     * per share class (here: the PCF plus the collateral API link of a synthetic ETF).
     */
    private static final String PART_HTML = """
        <div class="part-12610 part advancedhtml" data-part_id="12610">
        <script type="application/json" class="data">
        {
            "14653": [
                    {
                        name: "Liste aller Fondspositionen herunterladen",
                        url: "%s",
                        type: "csv"
                    }
            ],
            "15177": [
                    {
                        name: "Download collateral constituents",
                        url: "https://fundcentres.lgim.com/srp/api/fund-holdings-csv-download/137/",
                        type: "api"
                    }
                    ,
                    {
                        name: "Liste aller Fondspositionen herunterladen",
                        url: "%s",
                        type: "csv"
                    }
            ]
        }
        </script>
        </div>
        """.formatted(ACC_CSV_URL, CSV_URL);

    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private LGWebImporter importer;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).ignoreExpectOrder(true).build();
        importer = new LGWebImporter(restTemplate, new ObjectMapper());
    }

    /**
     * Builds a PCF with the header/trailer layout of the real file: metadata labels concatenated
     * with their value, a trailing note, a quoted disclaimer and the record count.
     */
    private String pcf(String tradingId, int recordCount, String... rows) {
        return "sep=,\n"
            + "Basket NameL&G Global Quality Dividends UCITS ETF - Dist\n"
            + "ETF Trading ID" + tradingId + "\n"
            + "Basket Trade Date2026-09-07\n"
            + "Security Description,ISIN,Trading Currency,Constituent Weight (Base)\n"
            + String.join("\n", rows) + "\n"
            + "\n"
            + "Cash Component is a balancing number.  Formula = \"1 minus the sum of constituent weights\"\n"
            + "\n"
            + "\"Basket Client DisclaimerThe weightings and holdings of the ETF may differ, from time to time.\"\n"
            + "Record Count" + recordCount + "\n";
    }

    private void expectCatalogAndPart() {
        server.expect(requestTo(CATALOG_URL))
            .andRespond(withSuccess(CATALOG_JSON, MediaType.APPLICATION_JSON));
        server.expect(requestTo(PART_URL))
            .andRespond(withSuccess(PART_HTML, MediaType.TEXT_HTML));
    }

    private void expectCsv(String url, String body) {
        server.expect(requestTo(url))
            .andRespond(withSuccess(body.getBytes(StandardCharsets.UTF_8),
                MediaType.parseMediaType("text/csv")));
    }

    @Test
    @DisplayName("resolves ISIN via catalogue and part fragment and converts weights to percent")
    void resolvesAndParses() {
        expectCatalogAndPart();
        expectCsv(CSV_URL, pcf(ISIN_DIST, 3,
            "JYSKE BANK A/S DKK 10.0,DK0010307958,DKK,0.35",
            "LENOVO GROUP LTD NPV,HK0992009065,HKD,0.5",
            "ENGIE,FR0010208488,EUR,0.148"));

        List<AllocationEntry> entries = importer.fetchAndParse(ISIN_DIST);

        server.verify();
        assertThat(entries).hasSize(3);

        Map<String, AllocationEntry> byIsin = entries.stream()
            .collect(Collectors.toMap(AllocationEntry::getIsin, Function.identity()));

        // 0.35 -> 35 %, weights are fractions of 1 in the source file
        assertThat(byIsin.get("DK0010307958").getPercentage()).isEqualByComparingTo("35");
        assertThat(byIsin.get("HK0992009065").getPercentage()).isEqualByComparingTo("50");
        assertThat(byIsin.get("FR0010208488").getPercentage()).isEqualByComparingTo("14.8");

        assertThat(byIsin.get("DK0010307958").getName()).isEqualTo("JYSKE BANK A/S DKK 10.0");
        // Country is derived from the ISIN prefix; the PCF carries no sector
        assertThat(byIsin.get("HK0992009065").getCountry()).isEqualTo("HK");
        assertThat(byIsin.get("HK0992009065").getSector()).isNull();
    }

    @Test
    @DisplayName("picks the csv document, not the collateral api link, of a share class")
    void picksCsvDocument() {
        expectCatalogAndPart();
        expectCsv(CSV_URL, pcf(ISIN_DIST, 1, "ENGIE,FR0010208488,EUR,0.995"));

        assertThat(importer.fetchAndParse(ISIN_DIST)).hasSize(1);
        server.verify();
    }

    @Test
    @DisplayName("resolves a different share class of the same fund to its own document")
    void resolvesSecondShareClass() {
        expectCatalogAndPart();
        expectCsv(ACC_CSV_URL, pcf(ISIN_ACC, 1, "ENGIE,FR0010208488,EUR,0.995"));

        assertThat(importer.fetchAndParse(ISIN_ACC)).hasSize(1);
        server.verify();
    }

    @Test
    @DisplayName("groups holdings without ISIN as 'Sonstige'")
    void groupsEntriesWithoutIsin() {
        expectCatalogAndPart();
        expectCsv(CSV_URL, pcf(ISIN_DIST, 3,
            "ENGIE,FR0010208488,EUR,0.9",
            "SOME UNLISTED POSITION,,EUR,0.06",
            "ANOTHER ONE,,USD,0.04"));

        List<AllocationEntry> entries = importer.fetchAndParse(ISIN_DIST);

        assertThat(entries).hasSize(2);
        AllocationEntry sonstige = entries.get(entries.size() - 1);
        assertThat(sonstige.getIsin()).isEqualTo("SONSTIGE00000");
        assertThat(sonstige.getName()).isEqualTo("Sonstige");
        assertThat(sonstige.getPercentage()).isEqualByComparingTo("10");
    }

    @Test
    @DisplayName("accepts a cash component, i.e. weights summing to slightly below 100%")
    void acceptsCashComponent() {
        expectCatalogAndPart();
        // 99.78 % - the remaining 0.22 % is the cash component and has no row of its own
        expectCsv(CSV_URL, pcf(ISIN_DIST, 2,
            "ENGIE,FR0010208488,EUR,0.5",
            "LENOVO GROUP LTD NPV,HK0992009065,HKD,0.4978"));

        List<AllocationEntry> entries = importer.fetchAndParse(ISIN_DIST);

        BigDecimal total = entries.stream()
            .map(AllocationEntry::getPercentage)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(total).isEqualByComparingTo("99.78");
    }

    @Test
    @DisplayName("throws when the holdings file belongs to a different ISIN")
    void throwsOnIsinMismatch() {
        expectCatalogAndPart();
        expectCsv(CSV_URL, pcf("IE00B3CNHJ55", 1, "ENGIE,FR0010208488,EUR,0.995"));

        assertThatThrownBy(() -> importer.fetchAndParse(ISIN_DIST))
            .isInstanceOf(InvalidFileFormatException.class)
            .hasMessageContaining("IE00B3CNHJ55");
    }

    @Test
    @DisplayName("throws when the record count does not match the parsed rows")
    void throwsOnRecordCountMismatch() {
        expectCatalogAndPart();
        expectCsv(CSV_URL, pcf(ISIN_DIST, 5,
            "ENGIE,FR0010208488,EUR,0.5",
            "LENOVO GROUP LTD NPV,HK0992009065,HKD,0.495"));

        assertThatThrownBy(() -> importer.fetchAndParse(ISIN_DIST))
            .isInstanceOf(InvalidFileFormatException.class)
            .hasMessageContaining("incomplete");
    }

    @Test
    @DisplayName("throws when the weights do not add up to roughly 100%")
    void throwsOnBadAllocationSum() {
        expectCatalogAndPart();
        expectCsv(CSV_URL, pcf(ISIN_DIST, 1, "ENGIE,FR0010208488,EUR,0.5"));

        assertThatThrownBy(() -> importer.fetchAndParse(ISIN_DIST))
            .isInstanceOf(AllocationSumException.class);
    }

    @Test
    @DisplayName("throws when the ISIN is unknown to the L&G catalogue")
    void throwsOnUnknownIsin() {
        server.expect(requestTo(CATALOG_URL))
            .andRespond(withSuccess(CATALOG_JSON, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> importer.fetchAndParse("DE0005557508"))
            .isInstanceOf(InvalidFileFormatException.class)
            .hasMessageContaining("DE0005557508");
    }

    @Test
    @DisplayName("throws when the downloaded file is not an L&G holdings file")
    void throwsOnForeignFile() {
        expectCatalogAndPart();
        expectCsv(CSV_URL, "Security Description,ISIN,Trading Currency,Constituent Weight (Base)\n"
            + "ENGIE,FR0010208488,EUR,0.995\n");

        assertThatThrownBy(() -> importer.fetchAndParse(ISIN_DIST))
            .isInstanceOf(InvalidFileFormatException.class)
            .hasMessageContaining("ETF Trading ID");
    }

    @Test
    @DisplayName("throws on the empty body of an AWS WAF rate-limit challenge")
    void throwsOnEmptyWafResponse() {
        server.expect(requestTo(CATALOG_URL))
            .andRespond(withStatus(org.springframework.http.HttpStatus.ACCEPTED).body(new byte[0]));

        assertThatThrownBy(() -> importer.fetchAndParse(ISIN_DIST))
            .isInstanceOf(InvalidFileFormatException.class)
            .hasMessageContaining("empty response");
    }

    @Test
    @DisplayName("keeps UTF-8 security names although text/csv carries no charset")
    void decodesUtf8() {
        expectCatalogAndPart();
        expectCsv(CSV_URL, pcf(ISIN_DIST, 1, "SOCIÉTÉ GÉNÉRALE SA,FR0000130809,EUR,0.995"));

        List<AllocationEntry> entries = importer.fetchAndParse(ISIN_DIST);

        assertThat(entries.get(0).getName()).isEqualTo("SOCIÉTÉ GÉNÉRALE SA");
    }

    @Test
    @DisplayName("reports the importer name")
    void reportsImporterName() {
        assertThat(importer.getImporterName()).isEqualTo("L&G Web");
    }
}
