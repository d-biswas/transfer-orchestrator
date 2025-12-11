package com.company.orchestrator.infrastructure.events.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.io.Serial;

/**
 * Event fired when contract negotiation starts
 * Topic: transfer.negotiation.started
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class TransferNegotiationStartedEvent extends BaseTransferEvent {
    @Serial
    private static final long serialVersionUID = 1L;

    private String providerId;
    private String assetId;

    @Override
    public String getEventType() {
        return "transfer.negotiation.started";
    }
}