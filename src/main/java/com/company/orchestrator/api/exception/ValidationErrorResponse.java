package com.company.orchestrator.api.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Response DTO for validation errors
 * Contains field-specific validation errors and global errors
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Validation error response with field-specific errors")
public class ValidationErrorResponse {

    @Schema(description = "Timestamp when the error occurred", example = "2024-01-15T10:30:00Z")
    private Instant timestamp;

    @Schema(description = "HTTP status code", example = "400")
    private int status;

    @Schema(description = "Error type", example = "Validation Failed")
    private String error;

    @Schema(description = "General error message", example = "Request validation failed")
    private String message;

    @Schema(description = "Field-specific validation errors", example = "{\"assetId\": \"Asset ID is required\", \"providerUrl\": \"Provider URL is not a valid URL\"}")
    private Map<String, String> fieldErrors;
}