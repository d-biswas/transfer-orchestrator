package com.company.orchestrator.api.response;

import com.company.orchestrator.domain.model.DataType;
import com.company.orchestrator.domain.model.TransferStatus;
import lombok.*;

import java.time.Instant;

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
    private Instant createdAt;
}
