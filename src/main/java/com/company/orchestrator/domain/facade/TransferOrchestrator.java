package com.company.orchestrator.domain.facade;

import com.company.orchestrator.api.request.DateParameters;
import com.company.orchestrator.api.request.TransferInitiateDto;
import com.company.orchestrator.api.response.PolicyEvaluationResponse;
import com.company.orchestrator.api.response.TransferDto;
import com.company.orchestrator.api.response.TransferResponseDto;
import com.company.orchestrator.audit.model.AuditEvent;
import com.company.orchestrator.audit.model.ComplianceReport;
import com.company.orchestrator.domain.model.TransferStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface TransferOrchestrator {

    /**
     * Initiates a data transfer with policy evaluation
     * @return TransferRequest ID and initial status
     */
    TransferResponseDto initiateTransfer(TransferInitiateDto
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
    List<AuditEvent> getTransferAuditLogs(Long transferId);

    /**
     * Retrieves page of transfers
     */
    Page<TransferDto> findTransfers(Pageable pageable);

    /**
     * Finds a transfer by its ID
     */
    Optional<TransferDto> findTransferById(Long transferId);

    /**
     * Generates compliance report
     */
    ComplianceReport getTransferAnalytics(DateParameters parameters);

    /**
     * Evaluates policies for a transfer request
     */
    PolicyEvaluationResponse evaluatePolicies(TransferInitiateDto request);
}
