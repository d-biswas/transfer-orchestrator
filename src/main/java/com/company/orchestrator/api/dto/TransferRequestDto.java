package com.company.orchestrator.api.dto;

import com.company.orchestrator.domain.model.DataType;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;


@Getter
@Setter
public class TransferRequestDto {
    private String transferId;           // Unique ID of the transfer
    private DataType dataType;             // e.g., "ProductionData", "QualityReport"
    private String consumerId;           // Who is receiving the data
    private String providerId;
    private String assetId;
    private String consumerRegion;       // Region of the consumer
    private String consumerCertification; // e.g., "ISO9001"
    private String usagePurpose;         // e.g., "QualityAnalysis"
    private long dataSize;               // in bytes
    private Instant requestTime;         // Timestamp of the request
}
