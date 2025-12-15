package com.company.orchestrator.api.request;

import com.company.orchestrator.api.validator.TransferInitiateRequestValidator;
import com.company.orchestrator.domain.model.DataType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

/**
 * Request DTO for initiating a data transfer
 * <p>
 * Validated by {@link TransferInitiateRequestValidator}
 * </p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to initiate a data transfer")
public class TransferInitiateDto {

    @Schema(description = "Asset ID to transfer", example = "asset-001", requiredMode = Schema.RequiredMode.REQUIRED)
    private String assetId;

    @Schema(description = "Provider participant ID", example = "provider-participant", requiredMode = Schema.RequiredMode.REQUIRED)
    private String providerId;

    @Schema(description = "Provider DSP endpoint", example = "http://provider-edc:7172/api/v1/dsp", requiredMode = Schema.RequiredMode.REQUIRED)
    private String providerUrl;

    @Schema(description = "Consumer participant ID", example = "consumer-participant", requiredMode = Schema.RequiredMode.REQUIRED)
    private String consumerId;

    @Schema(description = "Data type", example = "QUALITY", requiredMode = Schema.RequiredMode.REQUIRED)
    private DataType dataType;

    @Schema(description = "Consumer region for policy evaluation", example = "EU")
    private String consumerRegion;

    @Schema(description = "Consumer certification for policy evaluation", example = "ISO9001")
    private String consumerCertification;

    @Schema(description = "Data usage purpose for policy evaluation", example = "QualityAnalysis")
    private String usagePurpose;
}
