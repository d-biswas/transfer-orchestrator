package com.company.orchestrator.domain.service.impl;

import com.company.orchestrator.domain.model.DataType;
import com.company.orchestrator.domain.model.TransferStatus;
import com.company.orchestrator.domain.service.TransferStateService;
import com.company.orchestrator.infrastructure.persistence.entity.TransferRequestEntity;
import com.company.orchestrator.infrastructure.persistence.entity.TransferStateHistoryEntity;
import com.company.orchestrator.infrastructure.persistence.repository.TransferRequestRepository;
import com.company.orchestrator.infrastructure.persistence.repository.TransferStateHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Implementation of TransferStateService
 * Manages transfer lifecycle state transitions with full audit trail
 */
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TransferStateServiceImpl implements TransferStateService {

    private final TransferRequestRepository transferRequestRepository;
    private final TransferStateHistoryRepository stateHistoryRepository;

    /**
     * Get a transfer request by ID
     */
    @Override
    public TransferRequestEntity getTransferById(Long transferId) {
        return transferRequestRepository.findById(transferId)
                .orElseThrow(() -> new IllegalArgumentException("Transfer not found: " + transferId));
    }

    /**
     * Initiates a new transfer request
     * Creates transfer in REQUESTED state and records initial state transition
     */
    @Override
    @Transactional
    public TransferRequestEntity initiateTransfer(String consumerId, String providerId,
                                                   String assetId, DataType dataType) {
        log.info("Initiating transfer: consumerId={}, providerId={}, assetId={}, dataType={}",
                consumerId, providerId, assetId, dataType);

        Instant now = Instant.now();

        // Create transfer request entity
        TransferRequestEntity transfer = TransferRequestEntity.builder()
                .consumerId(consumerId)
                .providerId(providerId)
                .assetId(assetId)
                .dataType(dataType)
                .status(TransferStatus.REQUESTED)
                .createdAt(now)
                .modifiedAt(now)
                .build();

        transfer = transferRequestRepository.save(transfer);
        // Record initial state transition (null -> REQUESTED)
        recordStateTransition(transfer.getId(), null, TransferStatus.REQUESTED);

        log.info("Transfer initiated: transferId={}, status={}", transfer.getId(), transfer.getStatus());
        return transfer;
    }

    /**
     * Updates transfer state and records state transition
     * Ensures atomic update with version locking for concurrency safety
     */
    @Override
    @Transactional
    public void updateState(Long transferId, TransferStatus newStatus, String actor) {
        log.info("Updating transfer state: transferId={}, newStatus={}, actor={}",
                transferId, newStatus, actor);

        TransferRequestEntity transfer = transferRequestRepository.findById(transferId)
                .orElseThrow(() -> new IllegalArgumentException("Transfer not found: " + transferId));

        TransferStatus oldStatus = transfer.getStatus();
        validateStateTransition(oldStatus, newStatus);

        transfer.setStatus(newStatus);
        transfer.setModifiedAt(Instant.now());

        transferRequestRepository.save(transfer);
        recordStateTransition(transferId, oldStatus, newStatus);

        log.info("Transfer state updated: transferId={}, fromState={}, toState={}",
                transferId, oldStatus, newStatus);
    }

    /**
     * Cancels a transfer by updating state to CANCELLED
     */
    @Override
    @Transactional
    public void cancelTransfer(Long transferId, String actor) {
        log.info("Cancelling transfer: transferId={}, actor={}", transferId, actor);

        TransferRequestEntity transfer = transferRequestRepository.findById(transferId)
                .orElseThrow(() -> new IllegalArgumentException("Transfer not found: " + transferId));

        TransferStatus oldStatus = transfer.getStatus();
        transfer.setStatus(TransferStatus.CANCELLED);
        transfer.setModifiedAt(Instant.now());
        transferRequestRepository.save(transfer);

        recordStateTransition(transferId, oldStatus, TransferStatus.CANCELLED);
        log.info("Transfer cancelled: transferId={}, previousState={}", transferId, oldStatus);
    }

    /**
     * Gets current status of a transfer
     */
    @Override
    public TransferStatus getStatus(Long transferId) {
        log.debug("Getting transfer status: transferId={}", transferId);

        TransferRequestEntity transfer = transferRequestRepository.findById(transferId)
                .orElseThrow(() -> new IllegalArgumentException("Transfer not found: " + transferId));

        return transfer.getStatus();
    }

    /**
     * Gets complete state transition history for a transfer
     * Returns history in chronological order (oldest first)
     */
    @Override
    @Transactional(readOnly = true)
    public List<TransferStateHistoryEntity> getStateHistory(Long transferId) {
        log.debug("Getting state history: transferId={}", transferId);

        List<TransferStateHistoryEntity> history =
                stateHistoryRepository.findByTransferIdOrderByChangedAtAsc(transferId);

        log.debug("Retrieved {} state transitions for transferId={}", history.size(), transferId);
        return history;
    }

    /**
     * Records a state transition in the history table
     * Provides immutable audit trail for resilience and debugging
     */
    private void recordStateTransition(Long transferId, TransferStatus fromState, TransferStatus toState) {
        TransferStateHistoryEntity history = TransferStateHistoryEntity.builder()
                .transferId(transferId)
                .fromState(fromState)
                .toState(toState)
                .changedAt(Instant.now())
                .build();

        stateHistoryRepository.save(history);

        log.debug("Recorded state transition: transferId={}, fromState={}, toState={}",
                transferId, fromState, toState);
    }

    /**
     * Validates if a state transition is allowed
     * Prevents invalid state transitions (e.g., COMPLETED -> REQUESTED)
     */
    private void validateStateTransition(TransferStatus from, TransferStatus to) {
        // Allow transitions to the FAILED/CANCELLED from any state
        if (to == TransferStatus.FAILED || to == TransferStatus.CANCELLED) {
            return;
        }

        // Allow re-entry to same state (idempotency)
        if (from == to) {
            log.debug("Re-entering same state: {}", from);
            return;
        }

        // Validate state machine transitions based on lifecycle
        // REQUESTED → POLICY_EVALUATION → APPROVED/DENIED →
        // CONTRACT_NEGOTIATION → NEGOTIATED → TRANSFER_IN_PROGRESS → COMPLETED

        switch (from) {
            case REQUESTED:
                if (to != TransferStatus.POLICY_EVALUATION) {
                    throwInvalidTransition(from, to);
                }
                break;

            case POLICY_EVALUATION:
                if (to != TransferStatus.APPROVED && to != TransferStatus.DENIED) {
                    throwInvalidTransition(from, to);
                }
                break;

            case APPROVED:
                if (to != TransferStatus.CONTRACT_NEGOTIATION) {
                    throwInvalidTransition(from, to);
                }
                break;

            case DENIED:
                // Terminal state - no further transitions allowed
                throwInvalidTransition(from, to);
                break;

            case CONTRACT_NEGOTIATION:
                if (to != TransferStatus.NEGOTIATED) {
                    throwInvalidTransition(from, to);
                }
                break;

            case NEGOTIATED:
                if (to != TransferStatus.TRANSFER_IN_PROGRESS) {
                    throwInvalidTransition(from, to);
                }
                break;

            case TRANSFER_IN_PROGRESS:
                if (to != TransferStatus.COMPLETED) {
                    throwInvalidTransition(from, to);
                }
                break;

            case COMPLETED:
                // Terminal state - no transitions allowed
                throwInvalidTransition(from, to);
                break;

            case FAILED:
            case CANCELLED:
                // Terminal states - no transitions allowed
                throwInvalidTransition(from, to);
                break;

            default:
                log.warn("Unknown state transition validation: from={}, to={}", from, to);
        }
    }

    /**
     * Throws exception for invalid state transition
     */
    private void throwInvalidTransition(TransferStatus from, TransferStatus to) {
        String message = String.format("Invalid state transition: %s -> %s", from, to);
        log.error(message);
        throw new IllegalStateException(message);
    }

    /**
     * Updates EDC negotiation ID for a transfer
     * Called after EDC contract negotiation is initiated
     */
    @Override
    @Transactional
    public void updateEdcNegotiationId(Long transferId, String edcNegotiationId) {
        log.info("Updating EDC negotiation ID: transferId={}, negotiationId={}", transferId, edcNegotiationId);

        TransferRequestEntity transfer = transferRequestRepository.findById(transferId)
                .orElseThrow(() -> new IllegalArgumentException("Transfer not found: " + transferId));

        transfer.setEdcNegotiationId(edcNegotiationId);
        transfer.setModifiedAt(Instant.now());
        transferRequestRepository.save(transfer);
        log.debug("EDC negotiation ID updated: transferId={}", transferId);
    }

    /**
     * Updates EDC transfer process ID for a transfer
     * Called when EDC starts the data transfer process
     */
    @Override
    @Transactional
    public void updateEdcTransferProcessId(Long transferId, String edcTransferProcessId) {
        log.info("Updating EDC transfer process ID: transferId={}, edcTransferProcessId={}",
                transferId, edcTransferProcessId);

        TransferRequestEntity transfer = transferRequestRepository.findById(transferId)
                .orElseThrow(() -> new IllegalArgumentException("Transfer not found: " + transferId));

        transfer.setEdcTransferProcessId(edcTransferProcessId);
        transfer.setModifiedAt(Instant.now());
        transferRequestRepository.save(transfer);

        log.debug("EDC transfer process ID updated: transferId={}", transferId);
    }

    /**
     * Updates EDC contract agreement ID for a transfer
     * Called when EDC contract negotiation completes successfully
     */
    @Override
    @Transactional
    public void updateEdcContractAgreementId(Long transferId, String edcContractAgreementId) {
        log.info("Updating EDC contract agreement ID: transferId={}, edcContractAgreementId={}",
                transferId, edcContractAgreementId);

        TransferRequestEntity transfer = transferRequestRepository.findById(transferId)
                .orElseThrow(() -> new IllegalArgumentException("Transfer not found: " + transferId));

        transfer.setEdcContractAgreementId(edcContractAgreementId);
        transfer.setModifiedAt(Instant.now());
        transferRequestRepository.save(transfer);

        log.debug("EDC contract agreement ID updated: transferId={}", transferId);
    }
}