package com.stockstatus.service.importer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockstatus.dto.AllocationEntry;
import com.stockstatus.exception.InvalidFileFormatException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("ISharesWebImporter Unit Tests")
class ISharesWebImporterTest {

    private static final String PRODUCT_URL =
        "https://www.ishares.com/de/privatanleger/de/produkte/251973/ishares-stoxx-global-select-dividend-100-ucits-etf-de-fund";

    private static final String HOLDINGS_API_URL =
        "https://www.blackrock.com/varnish-api/uk-retail01-product-data/product-data/api/v2/get-product-data"
            + "?appType=PRODUCT_PAGE&appSubType=ISHARES&targetSite=de-ishares-v2&locale=de_DE&userType=individual"
            + "&portfolioId=251973&portfolioType=ISHARES_FUND_DATA&component=holdings&tab=all";

    /**
     * The holdings component props as the product page delivers them: a JSON blob inside an HTML
     * attribute, so every quote arrives as {@code &quot;}.
     */
    private static final String PRODUCT_PAGE = """
        <div data-componentname="holdings">
        <walrus-render-on-client componenttype="on-client-only" componentprops="{&quot;componentId&quot;:&quot;holdings&quot;,&quot;context&quot;:{&quot;productId&quot;:&quot;251973&quot;,&quot;portfolioType&quot;:&quot;ISHARES_FUND_DATA&quot;,&quot;currencyCode&quot;:&quot;EUR&quot;,&quot;ticker&quot;:&quot;ISPA&quot;,&quot;appSubType&quot;:&quot;ISHARES&quot;,&quot;locale&quot;:&quot;de_DE&quot;,&quot;targetSite&quot;:&quot;de-ishares-v2&quot;,&quot;userType&quot;:&quot;individual&quot;},&quot;tabs&quot;:[{&quot;name&quot;:&quot;all&quot;}]}" componentkey="HoldingsTable"></walrus-render-on-client>
        </div>
        """;

    /**
     * Column-oriented holdings response, trimmed to the columns the importer reads. Contains the
     * row shapes that need special handling: a non-breaking space in the sector name, an unmapped
     * sector, a cash row without ISIN, a futures row with a zero weight and an FX row with a
     * negative weight.
     */
    private static final String HOLDINGS_JSON = """
        {
          "productId": 251973,
          "fundName": "iShares STOXX Global Select Dividend 100 UCITS ETF (DE)",
          "componentsByNameMap": {
            "holdings": {
              "containersByNameMap": {
                "all": {
                  "dataPointsByNameMap": {
                    "issueName": {"value": [
                      "SITC INTERNATIONAL HOLDINGS LTD", "LEGAL AND GENERAL GROUP PLC",
                      "HOCHTIEF AG", "SOME EXOTIC HOLDING",
                      "EUR CASH", "EURO STOXX SELDIV 30 FUTURE DEC 26", "USD/EUR"]},
                    "isin": {"value": [
                      "KYG8187G1055", "GB0005603997",
                      "DE0006070006", "XX0000000000",
                      null, "DE000F3E19Q7", null]},
                    "holdingPercent": {"value": [
                      2.82872, 2.35705,
                      1.5, 0.5,
                      0.39045, 0.0, -0.00016]},
                    "sectorName": {"value": [
                      "Industrie", "Finanzwesen",
                      "Zyklische Konsumgüter ", "Wundertüte",
                      "Barmittel & Derivate", "Barmittel & Derivate", "Barmittel & Derivate"]},
                    "countryOfRisk": {"value": [
                      "Hongkong", "Vereinigtes Königreich",
                      "Deutschland", "Absurdistan",
                      "Europäische Union", null, "Europäische Union"]}
                  }
                }
              }
            }
          }
        }
        """;

    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private ISharesWebImporter importer;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).ignoreExpectOrder(true).build();
        importer = new ISharesWebImporter(restTemplate, new ObjectMapper());
    }

    private void expectProductPageAndHoldings() {
        server.expect(requestTo(PRODUCT_URL))
            .andRespond(withSuccess(PRODUCT_PAGE, MediaType.TEXT_HTML));
        server.expect(requestTo(HOLDINGS_API_URL))
            .andRespond(withSuccess(HOLDINGS_JSON, MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("Reads the API parameters off the product page and parses the holdings")
    void parsesHoldingsFromProductDataApi() {
        expectProductPageAndHoldings();

        List<AllocationEntry> entries = importer.fetchAndParse(PRODUCT_URL);

        server.verify();

        // The futures row (0%) and the FX row (negative) are dropped
        assertThat(entries).hasSize(5);
        assertThat(entries).extracting(AllocationEntry::getName)
            .containsExactly("SITC INTERNATIONAL HOLDINGS LTD", "LEGAL AND GENERAL GROUP PLC",
                "HOCHTIEF AG", "SOME EXOTIC HOLDING", "EUR CASH");

        Map<String, AllocationEntry> byName = entries.stream()
            .collect(Collectors.toMap(AllocationEntry::getName, Function.identity()));

        AllocationEntry sitc = byName.get("SITC INTERNATIONAL HOLDINGS LTD");
        assertThat(sitc.getIsin()).isEqualTo("KYG8187G1055");
        assertThat(sitc.getPercentage()).isEqualByComparingTo(new BigDecimal("2.82872"));
        assertThat(sitc.getSector()).isEqualTo("Industrials");
        assertThat(sitc.getCountry()).isEqualTo("HK");
        assertThat(sitc.getOriginalSector()).isNull();

        assertThat(byName.get("LEGAL AND GENERAL GROUP PLC").getSector()).isEqualTo("Financials");
        assertThat(byName.get("LEGAL AND GENERAL GROUP PLC").getCountry()).isEqualTo("GB");
    }

    @Test
    @DisplayName("Strips the non-breaking space iShares appends to some sector names")
    void mapsSectorPaddedWithNonBreakingSpace() {
        expectProductPageAndHoldings();

        AllocationEntry hochtief = importer.fetchAndParse(PRODUCT_URL).stream()
            .filter(entry -> "HOCHTIEF AG".equals(entry.getName()))
            .findFirst().orElseThrow();

        assertThat(hochtief.getSector()).isEqualTo("Consumer Discretionary");
        assertThat(hochtief.getOriginalSector()).isNull();
    }

    @Test
    @DisplayName("Reports unmapped sectors but not cash positions")
    void tracksUnmappedSectorsOnly() {
        expectProductPageAndHoldings();

        List<AllocationEntry> entries = importer.fetchAndParse(PRODUCT_URL);

        assertThat(entries).extracting(AllocationEntry::getOriginalSector)
            .filteredOn(sector -> sector != null)
            .containsExactly("Wundertüte");

        AllocationEntry cash = entries.stream()
            .filter(entry -> "EUR CASH".equals(entry.getName()))
            .findFirst().orElseThrow();
        assertThat(cash.getSector()).isEqualTo("Unbekannt");
        assertThat(cash.getOriginalSector()).isNull();
        assertThat(cash.getIsin()).isEmpty();
        assertThat(cash.getCountry()).isNull();
    }

    @Test
    @DisplayName("Fails with a readable message when the page carries no holdings component")
    void failsWhenHoldingsComponentIsMissing() {
        server.expect(requestTo(PRODUCT_URL))
            .andRespond(withSuccess("<html><body>Seite nicht gefunden</body></html>", MediaType.TEXT_HTML));

        assertThatThrownBy(() -> importer.fetchAndParse(PRODUCT_URL))
            .isInstanceOf(InvalidFileFormatException.class)
            .hasMessageContaining("Could not find the holdings component");
    }

    @Test
    @DisplayName("Fails when the holdings API answers with an error")
    void failsWhenHoldingsApiFails() {
        server.expect(requestTo(PRODUCT_URL))
            .andRespond(withSuccess(PRODUCT_PAGE, MediaType.TEXT_HTML));
        server.expect(requestTo(HOLDINGS_API_URL))
            .andRespond(withStatus(org.springframework.http.HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> importer.fetchAndParse(PRODUCT_URL))
            .isInstanceOf(InvalidFileFormatException.class)
            .hasMessageContaining("Failed to fetch data");
    }

    @Test
    @DisplayName("Fails when the holdings response has no holdings columns")
    void failsOnUnexpectedJsonStructure() {
        server.expect(requestTo(PRODUCT_URL))
            .andRespond(withSuccess(PRODUCT_PAGE, MediaType.TEXT_HTML));
        server.expect(requestTo(HOLDINGS_API_URL))
            .andRespond(withSuccess("{\"productId\": 251973}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> importer.fetchAndParse(PRODUCT_URL))
            .isInstanceOf(InvalidFileFormatException.class)
            .hasMessageContaining("no holdings data points found");
    }

    @Test
    @DisplayName("Rejects a missing web URL")
    void rejectsMissingWebUrl() {
        assertThatThrownBy(() -> importer.fetchAndParse("  "))
            .isInstanceOf(InvalidFileFormatException.class)
            .hasMessageContaining("No web URL provided");
    }
}
