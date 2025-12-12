package com.company.orchestrator.domain.facade;

import com.company.orchestrator.api.dto.TransferRequestDto;
import com.company.orchestrator.api.dto.TransferResultDto;
import com.company.orchestrator.audit.model.AuditEvent;
import com.company.orchestrator.domain.model.TransferStatus;

import java.util.List;

public interface TransferOrchestrator {

    /**
     * Initiates a data transfer with policy evaluation
     * @return TransferRequest ID and initial status
     */
    TransferResultDto initiateTransfer(TransferRequestDto
                                                   request);
    /**
     * Gets current status of a transfer
     */
    TransferStatus getTransferStatus(Long transferId);
    /**
     * Cancels an in-progress transfer
     */
    void cancelTransfer(Long transferId);
    /**
     * Retrieves audit log for a transfer
     */
    List<AuditEvent> getTransferAuditLog(Long transferId);
}
