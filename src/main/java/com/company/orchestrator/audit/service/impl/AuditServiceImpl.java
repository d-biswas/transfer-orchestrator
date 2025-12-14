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
    private static final String SYSTEM_ACTOR = "SYSTEM";

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

        AuditEventType eventType = result.allowed() ?
                AuditEventType.POLICY_EVALUATION_PASSED :
                AuditEventType.POLICY_EVALUATION_FAILED;

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
    public void logStateTransition(Long transferId, String consumerId, TransferStatus from, TransferStatus to) {
        log.info("Logging state transition for transfer: {} from {} to {}", transferId, from, to);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("fromState", from.getName());
        metadata.put("toState", to.getName());

        AuditLogEntity auditLog = AuditLogEntity.builder()
                .transferId(transferId)
                .consumerId(consumerId)
                .eventType(AuditEventType.STATE_CHANGED)
                .actor(SYSTEM_ACTOR)
                .message(String.format("State changed from %s to %s", from.getName(), to.getName()))
                .metadata(metadata)
                .createdAt(Instant.now())
                .build();

        auditLogRepository.save(auditLog);
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

        AuditEventType eventType = result.isSuccess() ?
                AuditEventType.TRANSFER_PROCESS_COMPLETED :
                AuditEventType.TRANSFER_PROCESS_FAILED;

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
                .findByTransferIdOrderByCreatedAtDesc(transferId);

        return entities.stream()
                .map(this::toAuditEvent)
                .collect(Collectors.toList());
    }

    @Override
    public ComplianceReport generateComplianceReport(DateRange range) {
        log.info("Generating compliance report from {} to {}", range.getFrom(), range.getTo());

        // For now, get all audit logs (in production, you'd filter by date)
        List<AuditLogEntity> allLogs = auditLogRepository.findAll();

        // Filter by date range
        List<AuditLogEntity> logsInRange = allLogs.stream()
                .filter(log -> !log.getCreatedAt().isBefore(range.getFrom())
                        && !log.getCreatedAt().isAfter(range.getTo()))
                .toList();

        // Calculate statistics
        long totalTransfers = logsInRange.stream()
                .filter(log -> log.getEventType() == AuditEventType.TRANSFER_REQUESTED)
                .count();

        long successfulTransfers = logsInRange.stream()
                .filter(log -> log.getEventType() == AuditEventType.TRANSFER_PROCESS_COMPLETED)
                .count();

        long failedTransfers = logsInRange.stream()
                .filter(log -> log.getEventType() == AuditEventType.TRANSFER_PROCESS_FAILED)
                .count();

        long deniedTransfers = logsInRange.stream()
                .filter(log -> log.getEventType() == AuditEventType.POLICY_EVALUATION_FAILED)
                .count();

        // Policy violation summary
        Map<String, Long> policyViolations = logsInRange.stream()
                .filter(log -> log.getEventType() == AuditEventType.POLICY_EVALUATION_FAILED)
                .collect(Collectors.groupingBy(
                        log -> (String) log.getMetadata().getOrDefault("policyType", "UNKNOWN"),
                        Collectors.counting()
                ));

        // Convert to audit events
        List<AuditEvent> events = logsInRange.stream()
                .map(this::toAuditEvent)
                .collect(Collectors.toList());

        return ComplianceReport.builder()
                .generatedAt(Instant.now())
                .dateRange(range)
                .totalTransfers(totalTransfers)
                .successfulTransfers(successfulTransfers)
                .failedTransfers(failedTransfers)
                .deniedTransfers(deniedTransfers)
                .policyViolations(policyViolations)
                .events(events)
                .build();
    }

    /**
     * Convert AuditLogEntity to AuditEvent model
     */
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