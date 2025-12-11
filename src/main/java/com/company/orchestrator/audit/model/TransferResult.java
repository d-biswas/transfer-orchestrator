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

    /** Whether transfer was successful */
    private boolean success;

    /** Number of bytes transferred */
    private Long bytesTransferred;

    /** Error message if failed */
    private String errorMessage;

    /** Error code if failed */
    private String errorCode;
}