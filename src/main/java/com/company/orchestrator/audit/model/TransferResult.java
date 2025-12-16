package com.company.orchestrator.audit.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Represents the final result of a transfer
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferResult {
    private boolean success;
    private Long bytesTransferred;
    private String errorMessage;
    private String errorCode;
}