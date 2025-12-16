package com.company.orchestrator.api.exception;

import com.company.orchestrator.api.error.ErrorReason;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.io.Serial;
import java.util.Map;

/**
 * Field validation exception
 */
@Getter
public class FieldValidationException extends ApiException {
    @Serial
    private static final long serialVersionUID = -3164157326834494969L;

    private final Map<String, String> validationErrors;

    public FieldValidationException(Map<String, String> validationErrors) {
        super(HttpStatus.BAD_REQUEST, ErrorReason.FIELD_VALIDATION_ERROR);
        this.validationErrors = validationErrors;
    }
}
