package com.company.orchestrator.infrastructure.config;

import com.company.orchestrator.infrastructure.props.EdcProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration for EDC Connector Client
 * Uses the shared WebClient and adds EDC-specific base URL and authentication
 */
@Configuration
@RequiredArgsConstructor
public class EdcConfig {

    private final EdcProperties edcProperties;
    private final WebClient webClient;

    /**
     * EDC WebClient - reuses shared WebClient configuration with EDC customizations
     */
    @Bean
    public WebClient edcWebClient() {
        return webClient.mutate()
                .baseUrl(edcProperties.getManagementUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("X-Api-Key", edcProperties.getApiKey())
                .build();
    }
}