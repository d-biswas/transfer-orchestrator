package com.company.orchestrator.infrastructure.edc.model;

import lombok.*;

/**
 * Request to initiate a data transfer via EDC
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferRequest {

    /**
     * Asset ID to transfer
     */
    private String assetId;

    /**
     * Provider's EDC DSP endpoint URL
     */
    private String providerUrl;

    /**
     * Data destination type (e.g., HttpProxy, S3, AzureBlob)
     */
    private String destinationType;

    /**
     * Destination endpoint URL where data should be sent
     */
    private String destinationUrl;

    /**
     * Protocol for transfer (default: dataspace-protocol-http)
     */
    private String protocol;

    /**
     * Whether transfer should be managed (true) or consumer pull (false)
     */
    private boolean managedTransfer;
}