package com.company.orchestrator.audit.service.impl;

import com.company.orchestrator.audit.model.AuditEvent;
import com.company.orchestrator.audit.model.ComplianceReport;
import com.company.orchestrator.audit.model.DateRange;
import com.company.orchestrator.audit.model.TransferResult;
import com.company.orchestrator.audit.service.AuditService;
import com.company.orchestrator.domain.model.AuditEventType;
import com.company.orchestrator.domain.model.TransferStatus;
import com.company.orchestrator.infrastructure.persistence.entity.AuditLogEntity;
import com.company.orchestrator.infrastructure.persistence.entity.TransferRequestEntity;
import com.company.orchestrator.infrastructure.persistence.repository.AuditLogRepository;
import com.company.orchestrator.policy.model.PolicyEvaluationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.company.orchestrator.utils.Constants.SYSTEM_ACTOR;

/**
 * Implementation of AuditService using AuditLogRepository
 * Provides immutable audit trail and compliance reporting
 */
@Log4j2
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional
    public void logTransferRequest(TransferRequestEntity request) {
        log.info("Logging transfer request: {}", request.getId());

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("assetId", request.getAssetId());
        metadata.put("providerId", request.getProviderId());
        metadata.put("dataType", request.getDataType().name());
        metadata.put("status", request.getStatus().getName());

        AuditLogEntity auditLog = AuditLogEntity.builder()
                .transferId(request.getId())
                .consumerId(request.getConsumerId())
                .eventType(AuditEventType.TRANSFER_REQUESTED)
                .actor(SYSTEM_ACTOR)
                .message(String.format("Transfer request from %s to %s for asset %s",
                        request.getConsumerId(), request.getProviderId(), request.getAssetId()))
                .metadata(metadata)
                .createdAt(Instant.now())
                .build();

        auditLogRepository.save(auditLog);
    }

    @Override
    @Transactional
    public void logPolicyEvaluation(Long transferId, String consumerId, PolicyEvaluationResult result) {
        log.info("Logging policy evaluation for transfer: {}, allowed: {}", transferId, result.allowed());

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("allowed", result.allowed());
        metadata.put("policyType", result.policy().getClass().getSimpleName());
        if (!result.allowed()) {
            metadata.put("violationReason", result.violationReason());
        }

        AuditEventType eventType = result.allowed() ? AuditEventType.POLICY_APPROVED : AuditEventType.POLICY_DENIED;
        AuditLogEntity auditLog = AuditLogEntity.builder()
                .transferId(transferId)
                .consumerId(consumerId)
                .eventType(eventType)
                .actor(SYSTEM_ACTOR)
                .message(result.allowed() ?
                        "Policy evaluation passed" :
                        "Policy evaluation failed: " + result.violationReason())
                .metadata(metadata)
                .createdAt(Instant.now())
                .build();

        auditLogRepository.save(auditLog);
    }

    @Override
    @Transactional
    public void logStateTransition(Long transferId, String consumerId, TransferStatus fromState, TransferStatus toState) {
        log.info("Logging state transition for transfer: {} from {} to {}", transferId, fromState, toState);

        AuditEventType eventType = mapStatusTransitionToEvent(fromState, toState);
        if (eventType != null) {
            AuditLogEntity log = AuditLogEntity.builder()
                    .transferId(transferId)
                    .consumerId(consumerId)
                    .eventType(eventType)
                    .actor(SYSTEM_ACTOR)
                    .message(String.format("Transitioned from %s to %s",
                            fromState != null ? fromState.getName() : "NULL",
                            toState.getName()))
                    .createdAt(Instant.now())
                    .build();
            auditLogRepository.save(log);
        }
    }

    @Override
    @Transactional
    public void logTransferCompletion(Long transferId, String consumerId, TransferResult result) {
        log.info("Logging transfer completion for: {}, success: {}", transferId, result.isSuccess());

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("success", result.isSuccess());
        if (result.getBytesTransferred() != null) {
            metadata.put("bytesTransferred", result.getBytesTransferred());
        }
        if (!result.isSuccess()) {
            metadata.put("errorMessage", result.getErrorMessage());
            metadata.put("errorCode", result.getErrorCode());
        }

        AuditEventType eventType = result.isSuccess() ? AuditEventType.TRANSFER_COMPLETED : AuditEventType.TRANSFER_FAILED;
        AuditLogEntity auditLog = AuditLogEntity.builder()
                .transferId(transferId)
                .consumerId(consumerId)
                .eventType(eventType)
                .actor(SYSTEM_ACTOR)
                .message(result.isSuccess() ?
                        "Transfer completed successfully" :
                        "Transfer failed: " + result.getErrorMessage())
                .metadata(metadata)
                .createdAt(Instant.now())
                .build();

        auditLogRepository.save(auditLog);
    }

    @Override
    public List<AuditEvent> getAuditLogsByTransferId(Long transferId) {
        log.debug("Fetching audit logs for the transfer: {}", transferId);

        List<AuditLogEntity> entities = auditLogRepository
                .findByTransferIdOrderByCreatedAtAsc(transferId);

        return entities.stream()
                .map(this::toAuditEvent)
                .collect(Collectors.toList());
    }

    @Override
    public ComplianceReport generateComplianceReport(DateRange dateRange) {
        return auditLogRepository.generateComplianceReport(dateRange);
    }

    private AuditEventType mapStatusTransitionToEvent(TransferStatus from, TransferStatus to) {
        if (from == null && to == TransferStatus.REQUESTED) {
            return AuditEventType.TRANSFER_REQUESTED;
        } else if (from == TransferStatus.REQUESTED && to == TransferStatus.POLICY_EVALUATION) {
            return AuditEventType.POLICY_EVALUATION_STARTED;
        } else if (from == TransferStatus.POLICY_EVALUATION && to == TransferStatus.APPROVED) {
            return AuditEventType.POLICY_APPROVED;
        } else if (from == TransferStatus.POLICY_EVALUATION && to == TransferStatus.DENIED) {
            return AuditEventType.POLICY_DENIED;
        } else if (from == TransferStatus.APPROVED && to == TransferStatus.CONTRACT_NEGOTIATION) {
            return AuditEventType.CONTRACT_NEGOTIATION_STARTED;
        } else if (from == TransferStatus.CONTRACT_NEGOTIATION && to == TransferStatus.NEGOTIATED) {
            return AuditEventType.CONTRACT_NEGOTIATED;
        } else if (from == TransferStatus.NEGOTIATED && to == TransferStatus.TRANSFER_IN_PROGRESS) {
            return AuditEventType.TRANSFER_STARTED;
        } else if (from == TransferStatus.TRANSFER_IN_PROGRESS && to == TransferStatus.COMPLETED) {
            return AuditEventType.TRANSFER_COMPLETED;
        } else if (from == TransferStatus.TRANSFER_IN_PROGRESS && to == TransferStatus.FAILED) {
            return AuditEventType.TRANSFER_FAILED;
        } else if (to == TransferStatus.CANCELLED) {
            return AuditEventType.TRANSFER_CANCELLED;
        }
        return null;
    }

    private AuditEvent toAuditEvent(AuditLogEntity entity) {
        return AuditEvent.builder()
                .id(entity.getId())
                .transferId(entity.getTransferId())
                .consumerId(entity.getConsumerId())
                .eventType(entity.getEventType())
                .actor(entity.getActor())
                .message(entity.getMessage())
                .metadata(entity.getMetadata())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}