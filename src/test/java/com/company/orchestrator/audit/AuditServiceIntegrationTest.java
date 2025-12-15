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
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
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
        // Save multiple audit logs for different consumers and event types
        auditLogRepository.save(AuditLogEntity.builder()
                .consumerId("consumer-01")
                .eventType(AuditEventType.TRANSFER_REQUESTED)
                .createdAt(Instant.now())
                .metadata(Map.of())
                .build());

        auditLogRepository.save(AuditLogEntity.builder()
                .consumerId("consumer-01")
                .eventType(AuditEventType.TRANSFER_PROCESS_COMPLETED)
                .createdAt(Instant.now())
                .metadata(Map.of())
                .build());

        auditLogRepository.save(AuditLogEntity.builder()
                .consumerId("consumer-02")
                .eventType(AuditEventType.POLICY_EVALUATION_FAILED)
                .createdAt(Instant.now())
                .metadata(Map.of())
                .build());

        DateRange range = DateRange.builder()
                .from(Instant.now().minusSeconds(3600))
                .to(Instant.now().plusSeconds(3600))
                .build();

        ComplianceReport report = auditService.generateComplianceReport(range);

        assertNotNull(report);
        assertEquals(2, report.getConsumerReports().size());

        // Verify consumer-01 counts
        var consumer1 = report.getConsumerReports().get("consumer-01");
        assertEquals(1L, consumer1.getTotalTransfers());
        assertEquals(1L, consumer1.getSuccessfulTransfers());

        // Verify consumer-02 counts
        var consumer2 = report.getConsumerReports().get("consumer-02");
        assertEquals(0L, consumer2.getTotalTransfers());
        assertEquals(0L, consumer2.getSuccessfulTransfers());
        assertEquals(1L, consumer2.getDeniedTransfers());
    }
}
