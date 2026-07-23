package com.stockstatus.service.importer;

import com.stockstatus.dto.AllocationEntry;
import com.stockstatus.exception.InvalidFileFormatException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Importer for Fidelity ETF holdings from web source
 * Downloads the Excel file from Fidelity's API and uses the FidelityExcelImporter for parsing
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FidelityWebImporter implements WebImporter {

    private static final String BASE_API_URL = "https://partner.fidelity.de/xapi/fund/portfolio/download/fundFullHolding";
    private static final String QUERY_PARAMS = "countries=de&country=de&languages=en&language=en&channels=ce.private-investor&channel=ce.private-investor";

    private final RestTemplate restTemplate;
    private final FidelityExcelImporter fidelityExcelImporter;

    /**
     * Fetch and parse Fidelity holdings using ISIN
     * Downloads the Excel file from Fidelity's API and delegates parsing to FidelityExcelImporter
     * @param isin The ETF ISIN (stored in ETF.isin field)
     * @return List of allocation entries
     */
    @Override
    public List<AllocationEntry> fetchAndParse(String isin) {
        log.info("Starting Fidelity web import for ISIN: {}", isin);

        try {
            // Build API URL with ISIN and current timestamp
            long timestamp = System.currentTimeMillis();
            String apiUrl = String.format("%s?id=%s&%s&r=%d", BASE_API_URL, isin, QUERY_PARAMS, timestamp);

            log.info("Fetching Excel file from: {}", apiUrl);

            // Fidelity sits behind Akamai bot protection, which rejects requests that don't look
            // like a real Chrome browser (403 "Access Denied"). A minimal User-Agent is not enough;
            // the full set of browser headers (plus HTTP/2, configured on the RestTemplate) is required.
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36");
            headers.set("Accept", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet,*/*;q=0.8");
            headers.set("Accept-Language", "de-DE,de;q=0.9,en-US;q=0.8,en;q=0.7");
            // Akamai requires an Accept-Encoding header, but we ask for "identity" so the response
            // is uncompressed xlsx that can be parsed directly (the JDK HttpClient does not
            // transparently decompress gzip).
            headers.set("Accept-Encoding", "identity");
            headers.set("sec-ch-ua", "\"Chromium\";v=\"126\", \"Not.A/Brand\";v=\"24\"");
            headers.set("sec-ch-ua-mobile", "?0");
            headers.set("sec-ch-ua-platform", "\"Windows\"");
            headers.set("Sec-Fetch-Dest", "document");
            headers.set("Sec-Fetch-Mode", "navigate");
            headers.set("Sec-Fetch-Site", "none");
            headers.set("Upgrade-Insecure-Requests", "1");
            headers.set("Referer", "https://partner.fidelity.de/");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            // Fetch the Excel file
            ResponseEntity<byte[]> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.GET,
                entity,
                byte[].class
            );

            if (response.getBody() == null || response.getBody().length == 0) {
                throw new InvalidFileFormatException("Fidelity Web",
                    "Received empty response from API endpoint: " + apiUrl);
            }

            byte[] excelData = response.getBody();
            log.info("Successfully fetched Excel file (size: {} bytes)", excelData.length);

            // Create a MultipartFile implementation from the downloaded bytes
            MultipartFile multipartFile = new ByteArrayMultipartFile(
                excelData,
                "holdings.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            );

            // Delegate parsing to FidelityExcelImporter
            log.info("Delegating parsing to FidelityExcelImporter");
            List<AllocationEntry> entries = fidelityExcelImporter.parseFile(multipartFile);

            log.info("Successfully parsed {} allocation entries from Excel file", entries.size());
            return entries;

        } catch (InvalidFileFormatException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to fetch data for ISIN: {}", isin, e);
            throw new InvalidFileFormatException("Fidelity Web",
                "Failed to fetch or parse data. Error: " + e.getMessage());
        }
    }

    @Override
    public String getImporterName() {
        return "Fidelity Web";
    }

    /**
     * Simple MultipartFile implementation backed by a byte array
     */
    private static class ByteArrayMultipartFile implements MultipartFile {
        private final byte[] content;
        private final String name;
        private final String contentType;

        public ByteArrayMultipartFile(byte[] content, String name, String contentType) {
            this.content = content;
            this.name = name;
            this.contentType = contentType;
        }

        @Override
        public String getName() {
            return "file";
        }

        @Override
        public String getOriginalFilename() {
            return name;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return content.length == 0;
        }

        @Override
        public long getSize() {
            return content.length;
        }

        @Override
        public byte[] getBytes() {
            return content;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(content);
        }

        @Override
        public void transferTo(File dest) throws IOException, IllegalStateException {
            throw new UnsupportedOperationException("transferTo not supported");
        }
    }
}
