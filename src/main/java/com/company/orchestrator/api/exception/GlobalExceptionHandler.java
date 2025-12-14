package com.company.orchestrator.api.exception;

import com.company.orchestrator.api.error.ApiError;
import com.company.orchestrator.api.error.ErrorReason;
import com.company.orchestrator.api.error.FieldValidationError;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Log4j2
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    @ExceptionHandler({ApiException.class})
    public ResponseEntity<ApiError> handleApiExceptions(ApiException e) {
        return new ResponseEntity<>(new ApiError(e.getReason(), e.getStatus()), e.getStatus());
    }

    @ExceptionHandler(FieldValidationException.class)
    public ResponseEntity<FieldValidationError> handleFieldValidationExceptions(FieldValidationException e) {
        return new ResponseEntity<>(new FieldValidationError(e.getValidationErrors(), e.getReason()), e.getStatus());
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleProductNotFoundException(NotFoundException ex) {
        log.error(ex.getMessage(), ex);
        return new ResponseEntity<>(new ApiError(ErrorReason.TRANSFER_REQUEST_NOT_FOUND, HttpStatus.NOT_FOUND, ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleAllOtherExceptions(Exception ex) {
        log.error("Unexpected error happened", ex);
        return new ResponseEntity<>(new ApiError(ErrorReason.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<FieldValidationError> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> errors = new HashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String field = violation.getPropertyPath().toString();
            errors.put(field, violation.getMessage());
        }
        return new ResponseEntity<>(
                new FieldValidationError(errors, ErrorReason.FIELD_VALIDATION_ERROR),
                HttpStatus.BAD_REQUEST
        );
    }
}
