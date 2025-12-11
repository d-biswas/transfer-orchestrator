package com.company.orchestrator.infrastructure.events.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.io.Serial;

/**
 * Event fired when transfer completes successfully
 * Topic: transfer.completed
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class TransferCompletedEvent extends BaseTransferEvent {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long bytesTransferred;
    private String edcTransferProcessId;

    @Override
    public String getEventType() {
        return "transfer.completed";
    }
}