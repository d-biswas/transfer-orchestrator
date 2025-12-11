package com.company.orchestrator.infrastructure.edc.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Result of contract negotiation with EDC
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractNegotiationResult {

    /**
     * Unique identifier for the negotiation process
     */
    private String negotiationId;

    /**
     * Contract agreement ID (only available after successful negotiation)
     */
    private String agreementId;

    /**
     * Current state of the negotiation
     * REQUESTED, OFFERED, ACCEPTED, AGREED, VERIFIED, FINALIZED, TERMINATED
     */
    private String state;

    /**
     * Error message if negotiation failed
     */
    private String errorMessage;

    /**
     * Whether the negotiation was successful
     */
    private boolean successful;

    public static ContractNegotiationResult success(String negotiationId, String agreementId, String state) {
        return ContractNegotiationResult.builder()
                .negotiationId(negotiationId)
                .agreementId(agreementId)
                .state(state)
                .successful(true)
                .build();
    }

    public static ContractNegotiationResult failure(String negotiationId, String state, String errorMessage) {
        return ContractNegotiationResult.builder()
                .negotiationId(negotiationId)
                .state(state)
                .errorMessage(errorMessage)
                .successful(false)
                .build();
    }
}