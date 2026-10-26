package com.stockstatus.integration;

import com.stockstatus.domain.ETF;
import com.stockstatus.domain.ETFAllocation;
import com.stockstatus.domain.ImporterType;
import com.stockstatus.domain.Stock;
import com.stockstatus.dto.AllocationEntry;
import com.stockstatus.repository.ETFAllocationRepository;
import com.stockstatus.repository.ETFRepository;
import com.stockstatus.repository.StockRepository;
import com.stockstatus.service.ETFService;
import com.stockstatus.service.importer.VanguardWebImporter;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@DisplayName("ETFService.refreshWebHoldings Integration Tests")
class RefreshWebHoldingsIntegrationTest extends BaseIntegrationTest {

    /** Roughly the size of the Vanguard FTSE Global All-Cap. */
    private static final int HOLDINGS = 7000;
    private static final int EXISTING = 3000;

    @Autowired
    private ETFService etfService;

    @Autowired
    private ETFRepository etfRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private ETFAllocationRepository etfAllocationRepository;

    @Autowired
    private EntityManager entityManager;

    @MockBean
    private VanguardWebImporter vanguardWebImporter;

    private static String isin(int i) {
        return String.format("US%09d0", i);
    }

    @Test
    @DisplayName("Should reuse existing stocks, create missing ones and save one allocation per holding")
    void shouldRefreshLargeEtf() {
        // Existing stocks: the first EXISTING ISINs, with an unknown sector to be filled in
        for (int i = 0; i < EXISTING; i++) {
            stockRepository.save(Stock.builder()
                .isin(isin(i)).name("Stock " + i).country("US").sector("Unbekannt").build());
        }
        // A stock known only by name (different ISIN), to be matched by name
        Stock byName = stockRepository.save(Stock.builder()
            .isin("DE0000000001").name("Name Match AG").country("DE").sector("Industrials").build());

        ETF etf = etfRepository.save(ETF.builder()
            .name("Vanguard FTSE Global All-Cap").isin("IE000VAHT5T0")
            .importerType(ImporterType.VANGUARD_WEB).build());

        List<AllocationEntry> entries = new ArrayList<>();
        BigDecimal weight = new BigDecimal("0.0142");
        for (int i = 0; i < HOLDINGS - 2; i++) {
            entries.add(AllocationEntry.builder()
                .isin(isin(i)).name("Stock " + i).percentage(weight)
                .country("US").sector("Information Technology").build());
        }
        entries.add(AllocationEntry.builder()
            .isin("").name("NAME MATCH AG").percentage(weight).country("DE").sector("Industrials").build());
        entries.add(AllocationEntry.builder()
            .isin("SONSTIGE00000").name("Sonstige").percentage(weight).build());
        when(vanguardWebImporter.fetchAndParse(anyString())).thenReturn(entries);
        entityManager.flush();
        entityManager.clear();

        long start = System.nanoTime();
        etfService.refreshWebHoldings(etf.getId());
        entityManager.flush();
        long millis = (System.nanoTime() - start) / 1_000_000;
        entityManager.clear();

        System.out.printf("refreshWebHoldings with %d holdings took %d ms%n", HOLDINGS, millis);

        List<ETFAllocation> allocations = etfAllocationRepository.findByEtfId(etf.getId());
        assertThat(allocations).hasSize(HOLDINGS);
        assertThat(allocations).allMatch(a -> a.getAllocationVersion() == 1);

        // EXISTING + name match + the ones created now (HOLDINGS - 2 - EXISTING) + Sonstige
        assertThat(stockRepository.count()).isEqualTo((long) HOLDINGS);
        assertThat(stockRepository.findByIsin(isin(0)).orElseThrow().getSector())
            .isEqualTo("Information Technology");
        assertThat(stockRepository.findByIsin(isin(HOLDINGS - 3))).isPresent();
        assertThat(allocations).anyMatch(a -> a.getStock().getId().equals(byName.getId()));

        // Guards against the per-entry query pattern, which took ~90 s in production
        assertThat(millis).isLessThan(30_000);
    }
}
