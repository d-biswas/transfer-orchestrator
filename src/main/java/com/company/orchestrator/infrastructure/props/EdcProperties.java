package com.company.orchestrator.infrastructure.props;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "edc.connector")
public class EdcProperties {
    /**
     * EDC Management API URL
     */
    private String managementUrl;

    /**
     * API Key for EDC authentication
     */
    private String apiKey;

    /**
     * Participant ID
     */
    private String participantId;

    /**
     * Negotiation timeout (seconds)
     */
    private int negotiationTimeoutSeconds = 300;

    /**
     * Polling interval (milliseconds)
     */
    private int pollingIntervalMillis = 1000;

    /**
     * Max retries for EDC calls
     */
    private int maxRetries = 3;

    /**
     * Retry backoff (milliseconds)
     */
    private int retryBackoffMillis = 2000;
}