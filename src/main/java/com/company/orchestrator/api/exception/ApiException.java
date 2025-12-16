package com.company.orchestrator.api.exception;

import com.company.orchestrator.api.error.ErrorReason;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;

import java.io.Serial;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base API exception
 */
@Getter
public class ApiException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = -700577521723422656L;

    private final HttpStatus status;
    private final ErrorReason reason;

    public ApiException(HttpStatus status, ErrorReason reason) {
        super();
        this.status = status;
        this.reason = reason;
    }

    public static ApiException notFound(ErrorReason reason) {
        return new ApiException(HttpStatus.NOT_FOUND, reason);
    }

    public static ApiException badRequest(ErrorReason reason) {
        return new ApiException(HttpStatus.BAD_REQUEST, reason);
    }

    public static ApiException forbidden(ErrorReason reason) {
        return new ApiException(HttpStatus.FORBIDDEN, reason);
    }

    public static ApiException fieldValidationError(List<FieldError> errors) {
        Map<String, String> validationErrors = new HashMap<>(0);
        for (FieldError fieldError : errors) {
            validationErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return new FieldValidationException(validationErrors);
    }
}
