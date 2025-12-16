package com.company.orchestrator.api.validator;

import com.company.orchestrator.api.request.TransferInitiateDto;
import jakarta.annotation.Nonnull;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import java.util.Set;

/**
 * Custom validator for TransferRequestDto
 * Performs field-level and cross-field validations for transfer requests
 */
@Log4j2
@Component
public class TransferInitiateRequestValidator implements Validator {

    private static final String FIELD_ASSET_ID = "assetId";
    private static final String FIELD_PROVIDER_ID = "providerId";
    private static final String FIELD_CONSUMER_ID = "consumerId";
    private static final String FIELD_DATA_TYPE = "dataType";
    private static final String FIELD_CONSUMER_REGION = "consumerRegion";

    private static final Set<String> VALID_REGIONS = Set.of(
            "EU", "DE", "FR"
    );

    private static final Set<String> VALID_CERTIFICATIONS = Set.of(
            "ISO9001", "ISO27001", "SOC2", "TISAX"
    );

    private static final int MAX_ASSET_ID_LENGTH = 255;
    private static final int MAX_PARTICIPANT_ID_LENGTH = 100;

    @Override
    public boolean supports(@Nonnull Class<?> clazz) {
        return TransferInitiateDto.class.equals(clazz);
    }

    @Override
    public void validate(@Nonnull Object target, @Nonnull Errors errors) {
        TransferInitiateDto request = (TransferInitiateDto) target;
        log.debug("Validating transfer request: assetId={}, providerId={}, consumerId={}",
                request.getAssetId(), request.getProviderId(), request.getConsumerId());

        validateRequiredFields(request, errors);
        validateFieldLengths(request, errors);
        validateBusinessRules(request, errors);
        validateOptionalFields(request, errors);

        log.debug("Validation completed. Errors: {}", errors.getErrorCount());
    }

    /**
     * Validates all required fields are present and not blank
     */
    private void validateRequiredFields(TransferInitiateDto request, Errors errors) {
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, FIELD_ASSET_ID, "field.required",
                "Asset ID is required");

        ValidationUtils.rejectIfEmptyOrWhitespace(errors, FIELD_PROVIDER_ID, "field.required",
                "Provider ID is required");

        ValidationUtils.rejectIfEmptyOrWhitespace(errors, FIELD_CONSUMER_ID, "field.required",
                "Consumer ID is required");

        if (request.getDataType() == null) {
            errors.rejectValue(FIELD_DATA_TYPE, "field.required", "Data type is required");
        }
    }

    /**
     * Validates field lengths don't exceed database constraints
     */
    private void validateFieldLengths(TransferInitiateDto request, Errors errors) {
        if (request.getAssetId() != null && request.getAssetId().length() > MAX_ASSET_ID_LENGTH) {
            errors.rejectValue(FIELD_ASSET_ID, "field.too.long",
                    String.format("Asset ID cannot exceed %d characters", MAX_ASSET_ID_LENGTH));
        }

        if (request.getProviderId() != null && request.getProviderId().length() > MAX_PARTICIPANT_ID_LENGTH) {
            errors.rejectValue(FIELD_PROVIDER_ID, "field.too.long",
                    String.format("Provider ID cannot exceed %d characters", MAX_PARTICIPANT_ID_LENGTH));
        }

        if (request.getConsumerId() != null && request.getConsumerId().length() > MAX_PARTICIPANT_ID_LENGTH) {
            errors.rejectValue(FIELD_CONSUMER_ID, "field.too.long",
                    String.format("Consumer ID cannot exceed %d characters", MAX_PARTICIPANT_ID_LENGTH));
        }
    }

    /**
     * Validates business rules and constraints
     */
    private void validateBusinessRules(TransferInitiateDto request, Errors errors) {
        // Example: Provider and consumer cannot be the same
        if (request.getProviderId() != null && request.getConsumerId() != null) {
            if (request.getProviderId().equals(request.getConsumerId())) {
                errors.reject("business.rule.violation",
                        "Provider and consumer cannot be the same participant");
            }
        }

        // Example: Asset ID should not contain special characters that could cause issues
        if (request.getAssetId() != null) {
            if (request.getAssetId().contains("..") || request.getAssetId().contains("//")) {
                errors.rejectValue(FIELD_ASSET_ID, "field.invalid.format",
                        "Asset ID contains invalid character sequences");
            }
        }
    }

    /**
     * Validates optional fields when provided
     */
    private void validateOptionalFields(TransferInitiateDto request, Errors errors) {
        // Validate consumer region if provided
        if (request.getConsumerRegion() != null && !request.getConsumerRegion().isBlank()) {
            String region = request.getConsumerRegion().toUpperCase();
            if (!VALID_REGIONS.contains(region)) {
                errors.rejectValue(FIELD_CONSUMER_REGION, "field.invalid.value",
                        String.format("Invalid region. Valid regions: %s", VALID_REGIONS));
            }
        }

        // Validate certification if provided
        if (request.getConsumerCertification() != null && !request.getConsumerCertification().isBlank()) {
            String certification = request.getConsumerCertification().toUpperCase();
            if (!VALID_CERTIFICATIONS.contains(certification)) {
                log.warn("Unknown certification type: {}", certification);
                // Just warn, don't reject - allow new certification types
            }
        }
    }
}