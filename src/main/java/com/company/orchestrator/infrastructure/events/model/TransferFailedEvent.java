package com.company.orchestrator.infrastructure.events.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.io.Serial;

/**
 * Event fired when transfer fails
 * Topic: transfer.failed
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class TransferFailedEvent extends BaseTransferEvent {
    @Serial
    private static final long serialVersionUID = 1L;

    private String errorMessage;
    private String errorCode;

    @Override
    public String getEventType() {
        return "transfer.failed";
    }
}