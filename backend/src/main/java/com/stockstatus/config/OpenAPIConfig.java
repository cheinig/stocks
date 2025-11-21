package com.stockstatus.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration
 */
@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI stockStatusOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Stock Status API")
                .description("REST API for Stock Status application - Portfolio management with ETF allocation tracking")
                .version("1.0.0")
                .contact(new Contact()
                    .name("Stock Status Team")
                    .email("support@stockstatus.com"))
                .license(new License()
                    .name("Apache 2.0")
                    .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
            .servers(List.of(
                new Server()
                    .url("http://localhost:8080")
                    .description("Local Development Server"),
                new Server()
                    .url("https://api.stockstatus.com")
                    .description("Production Server")))
            .tags(List.of(
                new Tag()
                    .name("Stocks")
                    .description("Operations related to stocks (individual securities)"),
                new Tag()
                    .name("ETFs")
                    .description("Operations related to ETFs (Exchange Traded Funds)"),
                new Tag()
                    .name("File Upload")
                    .description("File upload operations for ETF allocation data"),
                new Tag()
                    .name("Portfolios")
                    .description("Portfolio management operations"),
                new Tag()
                    .name("Dashboard")
                    .description("Dashboard and analytics endpoints")));
    }
}
