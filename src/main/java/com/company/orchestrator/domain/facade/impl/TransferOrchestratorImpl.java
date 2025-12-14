package com.company.orchestrator.domain.facade.impl;

import com.company.orchestrator.api.dto.TransferRequestDto;
import com.company.orchestrator.api.dto.TransferResultDto;
import com.company.orchestrator.audit.model.AuditEvent;
import com.company.orchestrator.audit.service.AuditService;
import com.company.orchestrator.domain.facade.TransferOrchestrator;
import com.company.orchestrator.domain.model.TransferStatus;
import com.company.orchestrator.domain.service.TransferStateService;
import com.company.orchestrator.infrastructure.edc.connector.EdcConnectorClient;
import com.company.orchestrator.infrastructure.edc.model.ContractNegotiationResult;
import com.company.orchestrator.infrastructure.edc.model.ContractOffer;
import com.company.orchestrator.infrastructure.events.model.TransferFailedErrorCode;
import com.company.orchestrator.infrastructure.persistence.entity.TransferRequestEntity;
import com.company.orchestrator.policy.model.PolicyEvaluationResult;
import com.company.orchestrator.policy.service.PolicyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.company.orchestrator.domain.utils.Constants.SYSTEM_ACTOR;

/**
 * Orchestrator facade for managing end-to-end transfer lifecycle
 * Coordinates policy evaluation, EDC contract negotiation, transfer execution, and event publishing
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class TransferOrchestratorImpl implements TransferOrchestrator {

    private final TransferStateService transferStateService;
    private final PolicyService policyService;
    private final EdcConnectorClient edcClient;
    private final AuditService auditService;

    /**
     * Initiates a data transfer with orchestration
     * <p>
     * Synchronous Flow (Orchestrator-managed):
     * REQUESTED → POLICY_EVALUATION → APPROVED/DENIED
     * <p>
     * Asynchronous Flow (Event-driven via Kafka/EDC callbacks):
     * CONTRACT_NEGOTIATION → NEGOTIATED → TRANSFER_IN_PROGRESS → COMPLETED/FAILED
     * <p>
     * After policy approval, EDC contract negotiation is initiated asynchronously.
     * All subsequent state updates happen via Kafka events from EtcCallbackController.
     */
    @Override
    @Transactional
    public TransferResultDto initiateTransfer(TransferRequestDto request) {
        log.info("Initiating transfer: consumerId={}, providerId={}, assetId={}, dataType={}",
                request.getConsumerId(), request.getProviderId(), request.getAssetId(), request.getDataType());

        TransferResultDto result = new TransferResultDto();
        try {
            // Create transfer request (REQUESTED)
            TransferRequestEntity transfer = transferStateService.initiateTransfer(
                    request.getConsumerId(),
                    request.getProviderId(),
                    request.getAssetId(),
                    request.getDataType()
            );

            Long transferId = transfer.getId();
            result.setTransferId(transferId);

            // Audit log transfer request
            auditService.logTransferRequest(transfer);
            log.info("Transfer request created: transferId={}, status={}", transferId, TransferStatus.REQUESTED);

            // Evaluate policies (POLICY_EVALUATION)
            transferStateService.updateState(transferId, TransferStatus.POLICY_EVALUATION, SYSTEM_ACTOR);
            log.info("Evaluating policies for transferId={}", transferId);

            PolicyEvaluationResult policyResult = policyService.evaluatePolicies(request);
            auditService.logPolicyEvaluation(transferId.toString(), policyResult);

            if (!policyResult.allowed()) {
                handlePolicyDenied(transferId, policyResult);
                result.setStatus(TransferStatus.DENIED);
                result.setMessage("Transfer denied: " + policyResult.violationReason());
                return result;
            }
            handlePolicyApproved(transferId, policyResult);

            // Initiate contract negotiation with EDC (ASYNC, NON-BLOCKING)
            transferStateService.updateState(transferId, TransferStatus.CONTRACT_NEGOTIATION, SYSTEM_ACTOR);
            auditService.logStateTransition(transferId.toString(), TransferStatus.APPROVED, TransferStatus.CONTRACT_NEGOTIATION);

            log.info("Starting async contract negotiation: transferId={}, providerId={}", transferId, request.getProviderId());

            ContractOffer contractOffer = buildContractOffer(request, transfer);

            // Call EDC asynchronously - this returns immediately with a process ID
            // EDC will send callbacks to /api/v1/transfers/callback with status updates
            ContractNegotiationResult negotiationResult = edcClient.negotiateContract(contractOffer);

            if (negotiationResult == null) {
                handleTransferFailed(transferId, "Failed to initiate contract negotiation",
                        TransferFailedErrorCode.EDC_NEGOTIATION_INITIATION_ERROR);
                result.setStatus(TransferStatus.FAILED);
                result.setMessage("Failed to initiate contract negotiation");
                return result;
            }

            log.info("Contract negotiation initiated asynchronously: transferId={}, negotiationId={}",
                    transferId, negotiationResult.getNegotiationId());

            // Store EDC negotiation ID for tracking and cancellation
            transferStateService.updateEdcNegotiationId(transferId, negotiationResult.getNegotiationId());

            // Return immediately - further updates will come via Kafka events
            result.setStatus(TransferStatus.CONTRACT_NEGOTIATION);
            result.setMessage(String.format(
                    "Transfer initiated successfully. Contract negotiation in progress. " +
                    "EDC Negotiation ID: %s. Further updates will be available via status endpoint.",
                    negotiationResult.getNegotiationId()
            ));

            log.info("Transfer initiation completed: transferId={}, currentState={}. " +
                    "Waiting for EDC callbacks for further state transitions.", transferId, result.getStatus());
            return result;

        } catch (Exception ex) {
            log.error("Error during transfer initiation: {}", ex.getMessage(), ex);
            if (result.getTransferId() != null) {
                handleTransferFailed(result.getTransferId(), ex.getMessage(),
                        TransferFailedErrorCode.ORCHESTRATION_ERROR);
            }

            result.setStatus(TransferStatus.FAILED);
            result.setMessage("Transfer initiation failed: " + ex.getMessage());
            return result;
        }
    }

    /**
     * Gets current status of a transfer
     */
    @Override
    public TransferStatus getTransferStatus(Long transferId) {
        log.debug("Getting transfer status: transferId={}", transferId);
        return transferStateService.getStatus(transferId);
    }

    /**
     * Cancels an in-progress transfer
     * Terminates EDC processes if transfer is in negotiation or transfer phase
     */
    @Override
    @Transactional
    public void cancelTransfer(Long transferId) {
        log.info("Cancelling transfer: transferId={}", transferId);

        try {
            TransferStatus currentStatus = transferStateService.getStatus(transferId);

            if (currentStatus == TransferStatus.COMPLETED ||
                currentStatus == TransferStatus.FAILED ||
                currentStatus == TransferStatus.CANCELLED) {
                log.warn("Cannot cancel transfer in status: transferId={}, status={}", transferId, currentStatus);
                throw new IllegalStateException("Transfer cannot be cancelled in current state: " + currentStatus);
            }
            TransferRequestEntity transferEntity = transferStateService.getTransferById(transferId);

            // If transfer is in EDC phase (negotiation or transfer in progress), terminate EDC process
            if (currentStatus == TransferStatus.CONTRACT_NEGOTIATION ||
                currentStatus == TransferStatus.NEGOTIATED ||
                currentStatus == TransferStatus.TRANSFER_IN_PROGRESS) {

                String processId = transferEntity.getEdcTransferProcessId();

                if (processId != null && !processId.isEmpty()) {
                    log.info("Terminating EDC transfer process: transferId={}, edcProcessId={}",
                            transferId, processId);

                    try {
                        edcClient.terminateTransfer(processId);
                        log.info("EDC termination request sent successfully: transferId={}, edcProcessId={}",
                                transferId, processId);
                    } catch (Exception edcEx) {
                        log.error("Failed to terminate EDC transfer process: transferId={}, edcProcessId={}, error={}",
                                transferId, processId, edcEx.getMessage());
                    }
                } else {
                    log.warn("No EDC process ID found for transfer in EDC phase: transferId={}, status={}",
                            transferId, currentStatus);
                }
            }
            log.info("Transfer cancelled successfully: transferId={}, previousStatus={}", transferId, currentStatus);

        } catch (IllegalArgumentException | IllegalStateException ex) {
            log.error("Cannot cancel transfer: transferId={}, error={}", transferId, ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Error cancelling transfer: transferId={}, error={}", transferId, ex.getMessage(), ex);
            throw new RuntimeException("Failed to cancel transfer: " + ex.getMessage(), ex);
        }
    }

    /**
     * Retrieves audit log for a transfer
     */
    @Override
    public List<AuditEvent> getTransferAuditLog(Long transferId) {
        log.debug("Retrieving audit log: transferId={}", transferId);
        return auditService.getAuditTrail(transferId.toString());
    }

    /**
     * Handle policy approval (synchronous, no Kafka events)
     */
    private void handlePolicyApproved(Long transferId, PolicyEvaluationResult policyResult) {
        transferStateService.updateState(transferId, TransferStatus.APPROVED, SYSTEM_ACTOR);
        auditService.logStateTransition(transferId.toString(), TransferStatus.POLICY_EVALUATION, TransferStatus.APPROVED);

        log.info("Policy approved: transferId={}, policyType={}", transferId, policyResult.policy().getClass().getSimpleName());
    }

    /**
     * Handle policy denial (synchronous, no Kafka events)
     */
    private void handlePolicyDenied(Long transferId, PolicyEvaluationResult policyResult) {
        transferStateService.updateState(transferId, TransferStatus.DENIED, SYSTEM_ACTOR);
        auditService.logStateTransition(transferId.toString(), TransferStatus.POLICY_EVALUATION, TransferStatus.DENIED);

        log.warn("Policy denied: transferId={}, policyType={}, reason={}",
                transferId, policyResult.policy().getClass().getSimpleName(), policyResult.violationReason());
    }

    /**
     * Handle transfer failure
     * Updates state, publishes failure event, and logs audit
     */
    private void handleTransferFailed(Long transferId, String errorMessage, TransferFailedErrorCode errorCode) {
        TransferRequestEntity transfer = transferStateService.getTransferById(transferId);
        TransferStatus oldStatus = transfer.getStatus();
        transferStateService.updateState(transferId, TransferStatus.FAILED, SYSTEM_ACTOR);
        auditService.logStateTransition(transferId.toString(), oldStatus, TransferStatus.FAILED);

        log.error("Transfer failed: transferId={}, errorMessage={}, errorCode={}",
                transferId, errorMessage, errorCode.getCode());
    }

    /**
     * Build contract offer for EDC negotiation
     * providerUrl: Provider's EDC DSP endpoint for contract negotiation
     * consumerCallbackUrl: Where provider EDC notifies about negotiation status
     */
    private ContractOffer buildContractOffer(TransferRequestDto request, TransferRequestEntity transfer) {
        return ContractOffer.builder()
                .providerId(request.getProviderId())
                .assetId(request.getAssetId())
                .providerUrl("http://provider-edc:8282/protocol")
                .offerId("offer-" + transfer.getId())
                .consumerCallbackUrl("http://transfer-orchestrator:8080/api/v1/transfers/callback")
                .build();
    }
}