package com.company.orchestrator.audit;

import com.company.orchestrator.audit.model.ComplianceReport;
import com.company.orchestrator.audit.model.DateRange;
import com.company.orchestrator.audit.service.impl.AuditServiceImpl;
import com.company.orchestrator.domain.model.AuditEventType;
import com.company.orchestrator.domain.model.DataType;
import com.company.orchestrator.domain.model.TransferStatus;
import com.company.orchestrator.infrastructure.persistence.entity.AuditLogEntity;
import com.company.orchestrator.infrastructure.persistence.entity.TransferRequestEntity;
import com.company.orchestrator.infrastructure.persistence.repository.AuditLogRepository;
import com.company.orchestrator.infrastructure.persistence.repository.TransferRequestRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataJpaTest
@Import({AuditServiceImpl.class, AuditServiceIntegrationTest.TestConfig.class})
@TestPropertySource(locations = "classpath:application.yml")
public class AuditServiceIntegrationTest {

    @Autowired
    private AuditServiceImpl auditService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private TransferRequestRepository transferRequestRepository;

    @TestConfiguration
    static class TestConfig {
        @Bean
        public JPAQueryFactory jpaQueryFactory(EntityManager em) {
            return new JPAQueryFactory(em);
        }
    }

    @Test
    void shouldSaveTransferRequestAuditLog() {
        TransferRequestEntity request = TransferRequestEntity.builder()
                .assetId("asset-001")
                .consumerId("consumer-01")
                .providerId("provider-01")
                .dataType(DataType.QUALITY)
                .status(TransferStatus.REQUESTED)
                .build();

        auditService.logTransferRequest(request);

        List<AuditLogEntity> logs = auditLogRepository.findAll();
        assertEquals(1, logs.size());

        AuditLogEntity saved = logs.getFirst();
        assertEquals("consumer-01", saved.getConsumerId());
        assertEquals(AuditEventType.TRANSFER_REQUESTED, saved.getEventType());
        assertEquals("provider-01", saved.getMetadata().get("providerId"));
    }

    @Test
    void shouldGenerateComplianceReport() {
        // Consumer-1: transferId = 1, REQUESTED -> COMPLETED
        TransferRequestEntity transfer1 = TransferRequestEntity.builder()
                .assetId("asset-001")
                .consumerId("consumer-01")
                .providerId("provider-01")
                .dataType(DataType.QUALITY)
                .status(TransferStatus.REQUESTED)
                .build();
        transfer1 = transferRequestRepository.save(transfer1);

        auditLogRepository.save(AuditLogEntity.builder()
                .transferId(transfer1.getId())
                .consumerId("consumer-01")
                .eventType(AuditEventType.TRANSFER_REQUESTED)
                .createdAt(Instant.now())
                .metadata(Map.of())
                .actor("SYSTEM")
                .build());

        auditLogRepository.save(AuditLogEntity.builder()
                .transferId(transfer1.getId())
                .consumerId("consumer-01")
                .eventType(AuditEventType.TRANSFER_PROCESS_COMPLETED)
                .createdAt(Instant.now())
                .metadata(Map.of())
                .actor("SYSTEM")
                .build());

        // Consumer-2: transferId = 2, REQUESTED -> POLICY_EVALUATION_FAILED
        TransferRequestEntity transfer2 = TransferRequestEntity.builder()
                .assetId("asset-002")
                .consumerId("consumer-02")
                .providerId("provider-02")
                .dataType(DataType.QUALITY)
                .status(TransferStatus.REQUESTED)
                .build();
        transfer2 = transferRequestRepository.save(transfer2);

        auditLogRepository.save(AuditLogEntity.builder()
                .transferId(transfer2.getId())
                .consumerId("consumer-02")
                .eventType(AuditEventType.TRANSFER_REQUESTED)
                .createdAt(Instant.now())
                .metadata(Map.of())
                .actor("SYSTEM")
                .build());

        auditLogRepository.save(AuditLogEntity.builder()
                .transferId(transfer2.getId())
                .consumerId("consumer-02")
                .eventType(AuditEventType.POLICY_EVALUATION_FAILED)
                .createdAt(Instant.now())
                .metadata(Map.of())
                .actor("SYSTEM")
                .build());

        // Date range covering all events
        DateRange range = DateRange.builder()
                .from(Instant.now().minusSeconds(3600))
                .to(Instant.now().plusSeconds(3600))
                .build();

        ComplianceReport report = auditService.generateComplianceReport(range);

        assertNotNull(report);
        assertEquals(2, report.getConsumerReports().size());

        // Verify consumer-01 counts
        var consumer1 = report.getConsumerReports().get("consumer-01");
        assertEquals(1L, consumer1.getTotalTransfers());      // 1 distinct transfer
        assertEquals(1L, consumer1.getSuccessfulTransfers()); // COMPLETED
        assertEquals(0L, consumer1.getFailedTransfers());
        assertEquals(0L, consumer1.getDeniedTransfers());

        // Verify consumer-02 counts
        var consumer2 = report.getConsumerReports().get("consumer-02");
        assertEquals(1L, consumer2.getTotalTransfers());      // 1 distinct transfer
        assertEquals(0L, consumer2.getSuccessfulTransfers());
        assertEquals(0L, consumer2.getFailedTransfers());
        assertEquals(1L, consumer2.getDeniedTransfers());     // POLICY_EVALUATION_FAILED
    }
}
