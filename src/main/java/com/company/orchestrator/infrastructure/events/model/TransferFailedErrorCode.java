package com.company.orchestrator.infrastructure.events.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Enumeration of error codes for transfer failures
 * Used to categorize and identify different types of transfer failures
 */
@Getter
@AllArgsConstructor
public enum TransferFailedErrorCode {
    EDC_NEGOTIATION_INITIATION_ERROR("EDC_NEGOTIATION_INITIATION_ERROR"),
    TRANSFER_INITIATION_FAILED("TRANSFER_INITIATION_FAILED"),
    TRANSFER_INITIATION_ERROR("TRANSFER_INITIATION_ERROR"),
    EDC_CALLBACK_FAILED("EDC_CALLBACK_FAILED"),
    EDC_CALLBACK_TERMINATED("EDC_CALLBACK_TERMINATED"),
    EDC_CALLBACK_ERROR("EDC_CALLBACK_ERROR"),
    DATA_VALIDATION_ERROR("DATA_VALIDATION_ERROR"),
    DATA_RECEPTION_ERROR("DATA_RECEPTION_ERROR"),
    ORCHESTRATION_ERROR("ORCHESTRATION_ERROR");

    private final String code;
}