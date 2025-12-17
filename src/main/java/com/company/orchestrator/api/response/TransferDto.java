package com.company.orchestrator.api.response;

import com.company.orchestrator.domain.model.DataType;
import com.company.orchestrator.domain.model.TransferStatus;
import com.company.orchestrator.infrastructure.utils.InstantDeserializer;
import com.company.orchestrator.infrastructure.utils.InstantSerializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.*;

import java.time.Instant;

/**
 * Data Transfer Object representing a Transfer
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransferDto {
    private Long id;
    private String consumerId;
    private String providerId;
    private String assetId;
    private DataType dataType;
    private TransferStatus status;
    private String edcNegotiationId;
    private String edcTransferProcessId;
    private String edcContractAgreementId;

    @JsonSerialize(using = InstantSerializer.class)
    @JsonDeserialize(using = InstantDeserializer.class)
    private Instant createdAt;
}
