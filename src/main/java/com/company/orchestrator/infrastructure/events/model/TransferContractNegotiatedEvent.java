package com.company.orchestrator.infrastructure.events.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * Event fired when transfer contract is successfully negotiated
 * Topic: transfer.contract-negotiated
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class TransferContractNegotiatedEvent extends BaseTransferEvent {
    @Serial
    private static final long serialVersionUID = 1L;

    /** Negotiation ID */
    private String agreementId;

    /** Correlation ID */
    private String correlationId;

    @Override
    public String getEventType() {
        return "contract.negotiated";
    }
}
