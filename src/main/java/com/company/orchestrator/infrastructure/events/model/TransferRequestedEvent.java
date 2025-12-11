package com.company.orchestrator.infrastructure.events.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.io.Serial;

/**
 * Event fired when a transfer is requested
 * Topic: transfer.requested
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class TransferRequestedEvent extends BaseTransferEvent {
    @Serial
    private static final long serialVersionUID = 1L;

    private String consumerId;
    private String providerId;
    private String assetId;
    private String dataType;

    @Override
    public String getEventType() {
        return "transfer.requested";
    }
}