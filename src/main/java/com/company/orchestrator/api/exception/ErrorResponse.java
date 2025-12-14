package com.company.orchestrator.api.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Generic error response DTO
 * Used for non-validation errors (business logic, server errors, etc.)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Generic error response")
public class ErrorResponse {

    @Schema(description = "Timestamp when the error occurred", example = "2024-01-15T10:30:00Z")
    private Instant timestamp;

    @Schema(description = "HTTP status code", example = "500")
    private int status;

    @Schema(description = "Error type", example = "Internal Server Error")
    private String error;

    @Schema(description = "Error message", example = "An unexpected error occurred")
    private String message;
}