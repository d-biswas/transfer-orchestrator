package com.company.orchestrator.infrastructure.props;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "edc.mock")
public class EdcProperties {

    /**
     * Base URL for callbacks to orchestrator
     */
    private String callbackBaseUrl;

    /**
     * Delay from REQUESTED to OFFERED state (milliseconds)
     */
    private long negotiationRequestedToOfferedDelayMs;

    /**
     * Delay from OFFERED to AGREED state (milliseconds)
     */
    private long negotiationOfferedToAgreedDelayMs;

    /**
     * Delay from AGREED to FINALIZED state (milliseconds)
     */
    private long negotiationAgreedToFinalizedDelayMs;

    /**
     * Delay from INITIAL to PROVISIONING state (milliseconds)
     */
    private long transferInitialToProvisioningDelayMs;

    /**
     * Delay from PROVISIONING to STARTED state (milliseconds)
     */
    private long transferProvisioningToStartedDelayMs;

    /**
     * Delay from STARTED to COMPLETED state (milliseconds)
     */
    private long transferStartedToCompletedDelayMs;

    /**
     * Failure simulation rate (0.0 to 1.0)
     * 0.0 = no failures, 0.1 = 10% failure rate, 1.0 = always fail
     */
    private double failureRate;

    /**
     * Thread pool size for scheduled callbacks
     */
    private int schedulerThreadPoolSize;
}
