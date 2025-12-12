package com.company.orchestrator.api.dto;

import com.company.orchestrator.domain.model.TransferStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransferResultDto {
    private Long transferId;
    private TransferStatus status;
    private String message;
}
