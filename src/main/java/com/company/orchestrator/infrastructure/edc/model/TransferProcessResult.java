package com.company.orchestrator.infrastructure.edc.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Result of transfer process initiation
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferProcessResult {

    /**
     * Unique identifier for the transfer process
     */
    private String transferProcessId;

    /**
     * Current state of the transfer
     */
    private String state;

    /**
     * Error message if transfer failed
     */
    private String errorMessage;

    /**
     * Whether the transfer initiation was successful
     */
    private boolean successful;

    public static TransferProcessResult success(String transferProcessId, String state) {
        return TransferProcessResult.builder()
                .transferProcessId(transferProcessId)
                .state(state)
                .successful(true)
                .build();
    }

    public static TransferProcessResult failure(String transferProcessId, String state, String errorMessage) {
        return TransferProcessResult.builder()
                .transferProcessId(transferProcessId)
                .state(state)
                .errorMessage(errorMessage)
                .successful(false)
                .build();
    }
}