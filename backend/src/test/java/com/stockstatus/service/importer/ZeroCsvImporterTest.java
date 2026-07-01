package com.stockstatus.service.importer;

import com.stockstatus.dto.PortfolioValueEntry;
import com.stockstatus.exception.InvalidFileFormatException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ZeroCsvImporter Unit Tests")
class ZeroCsvImporterTest {

    private static final String BOM = "﻿";
    private static final String HEADER =
        "Name;ISIN;WKN;Art;Anzahl;Verfügbar;Kaufkurs;Kaufwert;Kurs;Kurszeit;Kursdatum;Wert;Erfolg [%];Erfolg [EUR];Notiz";

    private final ZeroCsvImporter importer = new ZeroCsvImporter();

    private MultipartFile csv(String content) {
        return new MockMultipartFile(
            "file", "ZERO-pos.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("parses ISIN and Wert columns and rounds to full euros")
    void parsesAndRounds() {
        String content = BOM + HEADER + "\r\n"
            + "DT.TELEKOM AG NA;DE0005557508;555750;AKTIE;20;20;29,0965;581,93;24,23;11:47:56;01.07.2026;484,60;-16,73;-97,33;\r\n"
            + "ALLIANZ SE NA O.N.;DE0008404005;840400;AKTIE;1,123;1,123;366,10863758;411,14;411,60;11:46:43;01.07.2026;462,23;12,43;51,09;\r\n"
            + "RHEINMETALL AG;DE0007030009;703000;AKTIE;0,101;0,101;1.636,13861386;165,25;1.047,00;11:48:10;01.07.2026;105,75;-36,01;-59,50;\r\n";

        List<PortfolioValueEntry> entries = importer.parseFile(csv(content));

        Map<String, BigDecimal> byIsin = entries.stream()
            .collect(Collectors.toMap(PortfolioValueEntry::isin, PortfolioValueEntry::value));

        assertThat(entries).hasSize(3);
        // 484,60 -> 485, 462,23 -> 462, 105,75 -> 106
        assertThat(byIsin.get("DE0005557508")).isEqualByComparingTo("485");
        assertThat(byIsin.get("DE0008404005")).isEqualByComparingTo("462");
        assertThat(byIsin.get("DE0007030009")).isEqualByComparingTo("106");
    }

    @Test
    @DisplayName("handles German thousands separator in value column")
    void handlesThousandsSeparator() {
        String content = BOM + HEADER + "\r\n"
            + "SOME BIG POSITION;DE000TEST001;TEST01;AKTIE;10;10;100,00;1.000,00;123,45;11:00:00;01.07.2026;1.234,56;0,00;0,00;\r\n";

        List<PortfolioValueEntry> entries = importer.parseFile(csv(content));

        assertThat(entries).hasSize(1);
        // 1.234,56 -> 1235
        assertThat(entries.get(0).value()).isEqualByComparingTo("1235");
    }

    @Test
    @DisplayName("skips rows with empty ISIN or empty value")
    void skipsIncompleteRows() {
        String content = BOM + HEADER + "\r\n"
            + "VALID;DE0005557508;555750;AKTIE;20;20;29,00;581,00;24,00;11:00:00;01.07.2026;484,60;0,00;0,00;\r\n"
            + "NO ISIN;;;AKTIE;20;20;29,00;581,00;24,00;11:00:00;01.07.2026;100,00;0,00;0,00;\r\n"
            + "NO VALUE;DE000TEST002;TEST02;AKTIE;20;20;29,00;581,00;24,00;11:00:00;01.07.2026;;0,00;0,00;\r\n";

        List<PortfolioValueEntry> entries = importer.parseFile(csv(content));

        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).isin()).isEqualTo("DE0005557508");
    }

    @Test
    @DisplayName("throws when required columns are missing")
    void throwsWhenColumnsMissing() {
        String content = "Name;WKN;Anzahl\r\nFoo;123;10\r\n";

        assertThatThrownBy(() -> importer.parseFile(csv(content)))
            .isInstanceOf(InvalidFileFormatException.class)
            .hasMessageContaining("ISIN");
    }

    @Test
    @DisplayName("throws when file is empty")
    void throwsWhenEmpty() {
        MultipartFile empty = new MockMultipartFile("file", "ZERO-pos.csv", "text/csv", new byte[0]);

        assertThatThrownBy(() -> importer.parseFile(empty))
            .isInstanceOf(InvalidFileFormatException.class);
    }
}
