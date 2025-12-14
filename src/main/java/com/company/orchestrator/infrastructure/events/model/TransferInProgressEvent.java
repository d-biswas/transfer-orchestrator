package com.company.orchestrator.infrastructure.events.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.io.Serial;

/**
 * Event fired when transfer process starts (data transfer in progress)
 * Topic: transfer.in-progress
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class TransferInProgressEvent extends BaseTransferEvent {
    @Serial
    private static final long serialVersionUID = 6859899951912273577L;

    /** EDC transfer process ID */
    private String edcTransferProcessId;

    /** EDC state from callback */
    private String edcState;

    @Override
    public String getEventType() {
        return "transfer.in-progress";
    }
}