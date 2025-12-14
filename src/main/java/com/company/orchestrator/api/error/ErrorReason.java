package com.company.orchestrator.api.error;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorReason {
    FIELD_VALIDATION_ERROR("Field validation error"),
    TRANSFER_REQUEST_NOT_FOUND("Transfer request not found"),
    INTERNAL_SERVER_ERROR("We're sorry - the server encountered an unexpected error");

    private final String reason;
}
