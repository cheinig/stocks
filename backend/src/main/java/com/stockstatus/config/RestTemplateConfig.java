package com.stockstatus.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Configuration for RestTemplate.
 *
 * Uses the JDK {@link HttpClient} (via {@link JdkClientHttpRequestFactory}) instead of the
 * default {@code SimpleClientHttpRequestFactory}, because the latter only speaks HTTP/1.1.
 * Some upstreams sit behind Akamai and reject HTTP/1.1 requests with 403 (e.g. Fidelity's
 * holdings download), so we force HTTP/2 here.
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(30));

        return builder
                .requestFactory(() -> requestFactory)
                .build();
    }
}
