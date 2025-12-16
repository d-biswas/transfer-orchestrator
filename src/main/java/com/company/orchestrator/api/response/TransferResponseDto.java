package com.company.orchestrator.api.response;

import com.company.orchestrator.domain.model.TransferStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Transfer response DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransferResponseDto {
    private Long transferId;
    private TransferStatus status;
    private String message;
}
