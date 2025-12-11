package com.company.orchestrator.infrastructure.edc.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Current state of a transfer process in EDC
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferProcessState {

    /**
     * Transfer process ID
     */
    private String transferProcessId;

    /**
     * Current state
     * INITIAL, PROVISIONING, PROVISIONED, REQUESTING, REQUESTED, STARTED, COMPLETED, SUSPENDED, TERMINATED
     */
    private String state;

    /**
     * State timestamp
     */
    private Long stateTimestamp;

    /**
     * Error detail if any
     */
    private String errorDetail;

    /**
     * Data address for consumer pull (if available)
     */
    private String dataAddress;
}