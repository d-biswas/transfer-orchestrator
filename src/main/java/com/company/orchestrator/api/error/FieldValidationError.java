package com.company.orchestrator.api.error;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * Field validation error details
 */
@Getter
@Setter
public class FieldValidationError extends ApiError {
    private final Map<String, String> validationErrors;

    public FieldValidationError(Map<String, String> validationErrors, ErrorReason errorReason) {
        super(errorReason,  HttpStatus.BAD_REQUEST);
        this.validationErrors = validationErrors;
    }
}
