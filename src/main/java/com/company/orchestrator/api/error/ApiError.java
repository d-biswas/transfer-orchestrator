package com.company.orchestrator.api.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * API error response to be used in error handling
 */
@Getter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {
    private final String status;
    private final String message;
    private final ErrorReason errorReason;

    public ApiError(ErrorReason errorReason, HttpStatus httpStatus) {
        this.status = httpStatus.getReasonPhrase();
        this.message = errorReason.getReason();
        this.errorReason = errorReason;
    }

    public ApiError(ErrorReason errorReason, HttpStatus httpStatus, String message) {
        this.status = httpStatus.getReasonPhrase();
        this.message = message;
        this.errorReason = errorReason;
    }
}
