package com.company.orchestrator.infrastructure.edc.model;

import lombok.*;

/**
 * Represents a contract offer for EDC negotiation
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractOffer {

    /**
     * Provider's EDC DSP endpoint URL
     */
    private String providerUrl;

    /**
     * Provider's participant ID (BPN in Catena-X)
     */
    private String providerId;

    /**
     * Asset ID to be transferred
     */
    private String assetId;

    /**
     * Policy offer ID from the catalog
     */
    private String offerId;

    /**
     * Consumer's callback address for transfer completion
     */
    private String consumerCallbackUrl;
}