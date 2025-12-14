package com.company.orchestrator.audit.service;

import com.company.orchestrator.audit.model.AuditEvent;
import com.company.orchestrator.audit.model.ComplianceReport;
import com.company.orchestrator.audit.model.DateRange;
import com.company.orchestrator.audit.model.TransferResult;
import com.company.orchestrator.domain.model.TransferStatus;
import com.company.orchestrator.infrastructure.persistence.entity.TransferRequestEntity;
import com.company.orchestrator.policy.model.PolicyEvaluationResult;

import java.util.List;

/**
 * Service for managing audit trail and compliance reporting
 */
public interface AuditService {

    /**
     * Log a transfer request
     */
    void logTransferRequest(TransferRequestEntity request);

    /**
     * Log policy evaluation result
     */
    void logPolicyEvaluation(String transferId, PolicyEvaluationResult result);

    /**
     * Log transfer state transition
     */
    void logStateTransition(String transferId, TransferStatus from, TransferStatus to);

    /**
     * Log transfer completion
     */
    void logTransferCompletion(String transferId, TransferResult result);

    /**
     * Get complete audit logs for a transfer
     */
    List<AuditEvent> getAuditLogsByTransferId(String transferId);

    /**
     * Generate compliance report for a date range
     */
    ComplianceReport generateComplianceReport(DateRange range);
}
