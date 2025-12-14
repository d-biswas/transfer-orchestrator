package com.company.orchestrator.api.validator;

import com.company.orchestrator.api.dto.TransferRequestDto;
import jakarta.annotation.Nonnull;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;

/**
 * Custom validator for TransferRequestDto
 * Performs field-level and cross-field validations for transfer requests
 */
@Log4j2
@Component
public class TransferRequestValidator implements Validator {

    private static final Set<String> VALID_REGIONS = Set.of(
            "EU", "US", "ASIA", "APAC", "EMEA", "LATAM", "MEA"
    );

    private static final Set<String> VALID_CERTIFICATIONS = Set.of(
            "ISO9001", "ISO27001", "SOC2", "TISAX"
    );

    private static final int MAX_ASSET_ID_LENGTH = 255;
    private static final int MAX_PARTICIPANT_ID_LENGTH = 100;

    @Override
    public boolean supports(@Nonnull Class<?> clazz) {
        return TransferRequestDto.class.equals(clazz);
    }

    @Override
    public void validate(@Nonnull Object target, @Nonnull Errors errors) {
        TransferRequestDto request = (TransferRequestDto) target;

        log.debug("Validating transfer request: assetId={}, providerId={}, consumerId={}",
                request.getAssetId(), request.getProviderId(), request.getConsumerId());

        // 1. Required field validations
        validateRequiredFields(request, errors);

        // 2. Format validations
        if (!errors.hasFieldErrors("providerUrl")) {
            validateProviderUrl(request.getProviderUrl(), errors);
        }

        // 3. Length validations
        validateFieldLengths(request, errors);

        // 4. Business rule validations
        validateBusinessRules(request, errors);

        // 5. Optional field validations
        validateOptionalFields(request, errors);

        log.debug("Validation completed. Errors: {}", errors.getErrorCount());
    }

    /**
     * Validates all required fields are present and not blank
     */
    private void validateRequiredFields(TransferRequestDto request, Errors errors) {
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "assetId", "field.required",
                "Asset ID is required");

        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "providerId", "field.required",
                "Provider ID is required");

        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "providerUrl", "field.required",
                "Provider URL is required");

        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "consumerId", "field.required",
                "Consumer ID is required");

        if (request.getDataType() == null) {
            errors.rejectValue("dataType", "field.required", "Data type is required");
        }
    }

    /**
     * Validates provider URL is a valid HTTP/HTTPS URL and follows EDC DSP endpoint pattern
     */
    private void validateProviderUrl(String providerUrl, Errors errors) {
        if (providerUrl == null || providerUrl.isBlank()) {
            return; // Already validated in required fields
        }

        // Check if it's a valid URL
        try {
            URL url = new URL(providerUrl);

            // Validate protocol
            String protocol = url.getProtocol();
            if (!protocol.equals("http") && !protocol.equals("https")) {
                errors.rejectValue("providerUrl", "url.invalid.protocol",
                        "Provider URL must use HTTP or HTTPS protocol");
            }

            // Validate it looks like a DSP endpoint (should contain /dsp or /api/v1/dsp)
            if (!providerUrl.contains("/dsp")) {
                log.warn("Provider URL does not contain '/dsp' path: {}", providerUrl);
                errors.rejectValue("providerUrl", "url.invalid.format",
                        "Provider URL should be a DSP endpoint (e.g., http://provider-edc:7172/api/v1/dsp)");
            }

        } catch (MalformedURLException e) {
            errors.rejectValue("providerUrl", "url.malformed",
                    "Provider URL is not a valid URL: " + e.getMessage());
        }
    }

    /**
     * Validates field lengths don't exceed database constraints
     */
    private void validateFieldLengths(TransferRequestDto request, Errors errors) {
        if (request.getAssetId() != null && request.getAssetId().length() > MAX_ASSET_ID_LENGTH) {
            errors.rejectValue("assetId", "field.too.long",
                    String.format("Asset ID cannot exceed %d characters", MAX_ASSET_ID_LENGTH));
        }

        if (request.getProviderId() != null && request.getProviderId().length() > MAX_PARTICIPANT_ID_LENGTH) {
            errors.rejectValue("providerId", "field.too.long",
                    String.format("Provider ID cannot exceed %d characters", MAX_PARTICIPANT_ID_LENGTH));
        }

        if (request.getConsumerId() != null && request.getConsumerId().length() > MAX_PARTICIPANT_ID_LENGTH) {
            errors.rejectValue("consumerId", "field.too.long",
                    String.format("Consumer ID cannot exceed %d characters", MAX_PARTICIPANT_ID_LENGTH));
        }
    }

    /**
     * Validates business rules and constraints
     */
    private void validateBusinessRules(TransferRequestDto request, Errors errors) {
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
                errors.rejectValue("assetId", "field.invalid.format",
                        "Asset ID contains invalid character sequences");
            }
        }
    }

    /**
     * Validates optional fields when provided
     */
    private void validateOptionalFields(TransferRequestDto request, Errors errors) {
        // Validate consumer region if provided
        if (request.getConsumerRegion() != null && !request.getConsumerRegion().isBlank()) {
            String region = request.getConsumerRegion().toUpperCase();
            if (!VALID_REGIONS.contains(region)) {
                errors.rejectValue("consumerRegion", "field.invalid.value",
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