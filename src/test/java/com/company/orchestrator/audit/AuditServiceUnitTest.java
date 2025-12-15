package com.company.orchestrator.audit;

import com.company.orchestrator.audit.model.AuditEvent;
import com.company.orchestrator.audit.model.TransferResult;
import com.company.orchestrator.audit.service.impl.AuditServiceImpl;
import com.company.orchestrator.domain.model.AuditEventType;
import com.company.orchestrator.domain.model.DataType;
import com.company.orchestrator.domain.model.TransferStatus;
import com.company.orchestrator.infrastructure.persistence.entity.AuditLogEntity;
import com.company.orchestrator.infrastructure.persistence.entity.TransferRequestEntity;
import com.company.orchestrator.infrastructure.persistence.repository.AuditLogRepository;
import com.company.orchestrator.policy.model.Policy;
import com.company.orchestrator.policy.model.PolicyEvaluationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AuditServiceUnitTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    private AuditServiceImpl auditService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        auditService = new AuditServiceImpl(auditLogRepository);
    }

    @Test
    void shouldLogTransferRequest() {
        TransferRequestEntity request = TransferRequestEntity.builder()
                .assetId("asset-001")
                .consumerId("consumer-01")
                .providerId("provider-01")
                .dataType(DataType.QUALITY)
                .status(TransferStatus.REQUESTED)
                .build();

        auditService.logTransferRequest(request);

        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
        verify(auditLogRepository, times(1)).save(captor.capture());

        AuditLogEntity saved = captor.getValue();
        assertEquals("consumer-01", saved.getConsumerId());
        assertEquals(AuditEventType.TRANSFER_REQUESTED, saved.getEventType());
        assertEquals(request.getProviderId(), saved.getMetadata().get("providerId"));
    }

    @Test
    void shouldLogPolicyEvaluationPassed() {
        PolicyEvaluationResult result = new PolicyEvaluationResult(
                mock(com.company.orchestrator.policy.model.Policy.class),
                true,
                null
        );

        auditService.logPolicyEvaluation(1L, "consumer-01", result);

        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLogEntity saved = captor.getValue();
        assertEquals(AuditEventType.POLICY_EVALUATION_PASSED, saved.getEventType());
        assertEquals(true, saved.getMetadata().get("allowed"));
        assertTrue(saved.getMessage().contains("passed"));
    }

    @Test
    void shouldLogPolicyEvaluationFailed() {
        PolicyEvaluationResult result = new PolicyEvaluationResult
                (mock(Policy.class), false, "Rate limit exceeded");

        auditService.logPolicyEvaluation(1L, "consumer-01", result);

        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLogEntity saved = captor.getValue();
        assertEquals(AuditEventType.POLICY_EVALUATION_FAILED, saved.getEventType());
        assertEquals(false, saved.getMetadata().get("allowed"));
        assertEquals("Rate limit exceeded", saved.getMetadata().get("violationReason"));
        assertTrue(saved.getMessage().contains("failed"));
    }

    @Test
    void shouldLogStateTransition() {
        auditService.logStateTransition(1L, "consumer-01", TransferStatus.REQUESTED, TransferStatus.POLICY_EVALUATION);

        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLogEntity saved = captor.getValue();
        assertEquals(AuditEventType.STATE_CHANGED, saved.getEventType());
        assertEquals("REQUESTED", saved.getMetadata().get("fromState"));
        assertEquals("POLICY_EVALUATION", saved.getMetadata().get("toState"));
        assertTrue(saved.getMessage().contains("State changed"));
    }

    @Test
    void shouldLogTransferCompletionSuccess() {
        TransferResult result = TransferResult.builder()
                .success(true)
                .bytesTransferred(1024L)
                .build();

        auditService.logTransferCompletion(1L, "consumer-01", result);

        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLogEntity saved = captor.getValue();
        assertEquals(AuditEventType.TRANSFER_PROCESS_COMPLETED, saved.getEventType());
        assertEquals(true, saved.getMetadata().get("success"));
        assertEquals(1024L, saved.getMetadata().get("bytesTransferred"));
    }

    @Test
    void shouldLogTransferCompletionFailure() {
        TransferResult result = TransferResult.builder()
                .success(false)
                .errorMessage("File transfer failed")
                .errorCode("TRANSFER_TIMEOUT")
                .build();

        auditService.logTransferCompletion(1L, "consumer-01", result);

        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLogEntity saved = captor.getValue();
        assertEquals(AuditEventType.TRANSFER_PROCESS_FAILED, saved.getEventType());
        assertEquals(false, saved.getMetadata().get("success"));
        assertEquals("File transfer failed", saved.getMetadata().get("errorMessage"));
        assertEquals("TRANSFER_TIMEOUT", saved.getMetadata().get("errorCode"));
    }


    @Test
    void shouldReturnAuditLogsByTransferId() {
        AuditLogEntity entity = AuditLogEntity.builder()
                .transferId(1L)
                .consumerId("consumer-01")
                .eventType(AuditEventType.TRANSFER_REQUESTED)
                .message("Test")
                .metadata(Map.of())
                .createdAt(Instant.now())
                .build();

        when(auditLogRepository.findByTransferIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(entity));

        List<AuditEvent> events = auditService.getAuditLogsByTransferId(1L);
        assertEquals(1, events.size());
        assertEquals(AuditEventType.TRANSFER_REQUESTED, events.getFirst().getEventType());
    }

}
