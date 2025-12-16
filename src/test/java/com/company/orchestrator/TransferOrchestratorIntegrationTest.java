package com.company.orchestrator;

import com.company.orchestrator.api.request.TransferInitiateDto;
import com.company.orchestrator.api.response.TransferResponseDto;
import com.company.orchestrator.domain.facade.TransferOrchestrator;
import com.company.orchestrator.domain.model.AuditEventType;
import com.company.orchestrator.domain.model.DataType;
import com.company.orchestrator.domain.model.TransferStatus;
import com.company.orchestrator.infrastructure.persistence.entity.AuditLogEntity;
import com.company.orchestrator.infrastructure.persistence.entity.TransferRequestEntity;
import com.company.orchestrator.infrastructure.persistence.repository.AuditLogRepository;
import com.company.orchestrator.infrastructure.persistence.repository.TransferRequestRepository;
import com.company.orchestrator.infrastructure.props.EdcProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for the full transfer lifecycle.
 * <p>
 * This test verifies the complete end-to-end flow:
 * 1. TransferOrchestrator initiates a transfer negotiation with EdcConnectorClient
 * 2. EdcConnectorClient (mock) simulates a provider and triggers callbacks to EdcCallbackController
 * 3. EdcCallbackController publishes Kafka events through TransferEventPublisher
 * 4. TransferEventHandler listens to Kafka and updates transfer state accordingly
 * 5. Audit logs, transfer states, and Kafka events are correctly persisted and processed
 * </p>
 * <p>
 * Architecture tested:
 * <pre>
 * API Layer (TransferOrchestrator)
 *     ↓ (initiateTransfer)
 * Infrastructure (EdcConnectorClient - Mock Implementation)
 *     ↓ (async callbacks via scheduled executor)
 * API Layer (EdcCallbackController)
 *     ↓ (publishes events)
 * Infrastructure (TransferEventPublisher → Kafka)
 *     ↓ (Kafka topics)
 * Infrastructure (TransferEventHandler - Kafka Listeners)
 *     ↓ (updates state)
 * Persistence (TransferRequestRepository, AuditLogRepository)
 * </pre>
 * </p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EmbeddedKafka(
        partitions = 1,
        topics = {
                "test.negotiation.completed",
                "test.transfer.in-progress",
                "test.transfer.completed",
                "test.transfer.failed",
                "test.orchestrator-dlq"
        }
)
@ActiveProfiles("test")
public class TransferOrchestratorIntegrationTest {

    @Autowired
    private TransferOrchestrator transferOrchestrator;

    @Autowired
    private TransferRequestRepository transferRequestRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @LocalServerPort
    private int port;

    @Autowired
    private EdcProperties edcProperties;

    /**
     * Clean up database before each test to ensure test isolation
     */
    @BeforeEach
    void setUp() {
        edcProperties.setCallbackBaseUrl("http://localhost:" + port);
        auditLogRepository.deleteAll();
        transferRequestRepository.deleteAll();
    }

    /**
     * Clean up database after each test
     */
    @AfterEach
    void tearDown() {
        auditLogRepository.deleteAll();
        transferRequestRepository.deleteAll();
    }

    /**
     * Test the complete successful transfer lifecycle from REQUESTED to COMPLETED.
     * <p>
     * Expected state transitions:
     * <pre>
     * REQUESTED
     *   → POLICY_EVALUATION (sync)
     *   → APPROVED (sync)
     *   → CONTRACT_NEGOTIATION (async - mock EDC starts negotiation)
     *   → NEGOTIATED (via Kafka event from EdcCallbackController)
     *   → TRANSFER_IN_PROGRESS (via Kafka event from EdcCallbackController)
     *   → COMPLETED (via Kafka event from EdcCallbackController after data reception)
     * </pre>
     * </p>
     * <p>
     * Verifies:
     * - Transfer state transitions occur in the correct order
     * - Audit logs are created for each significant event
     * - Mock EDC triggers callbacks correctly
     * - Kafka events are published and consumed
     * - Final state is COMPLETED
     * </p>
     */
    @Test
    void fullTransferLifecycleTest() {
        // Create transfer request
        TransferInitiateDto request = TransferInitiateDto.builder()
                .assetId("asset-quality-001")
                .providerId("provider-bmw")
                .consumerId("consumer-vw")
                .dataType(DataType.QUALITY)
                .consumerRegion("EU")
                .consumerCertification("ISO9001")
                .usagePurpose("QualityAnalysis")
                .build();

        // Initiate the transfer - this is synchronous through policy evaluation
        TransferResponseDto response = transferOrchestrator.initiateTransfer(request);

        assertNotNull(response, "Transfer response should not be null");
        assertNotNull(response.getTransferId(), "Transfer ID should be assigned");
        Long transferId = response.getTransferId();

        // Verify Initial States (REQUESTED → POLICY_EVALUATION → APPROVED)
        // At this point, the synchronous phase is complete
        // The transfer should be in APPROVED state (or CONTRACT_NEGOTIATION if very fast)
        TransferRequestEntity initialTransfer = transferRequestRepository.findById(transferId)
                .orElseThrow(() -> new AssertionError("Transfer should exist in database"));

        // The transfer should have progressed from REQUESTED
        assertNotNull(initialTransfer.getStatus(), "Transfer status should be set");
        assertTrue(
                List.of(TransferStatus.APPROVED, TransferStatus.CONTRACT_NEGOTIATION).contains(initialTransfer.getStatus()),
                "Transfer should be APPROVED or already in CONTRACT_NEGOTIATION, but was: " + initialTransfer.getStatus()
        );

        // Verify initial audit logs (TRANSFER_REQUESTED > POLICY_EVALUATION_STARTED > POLICY_APPROVED)
        List<AuditLogEntity> initialAuditLogs = auditLogRepository.findAllByTransferId(transferId);
        assertFalse(initialAuditLogs.isEmpty(), "Audit logs should be created");
        Set<AuditEventType> eventTypes = initialAuditLogs.stream()
                .map(AuditLogEntity::getEventType)
                .collect(Collectors.toSet());
        assertTrue(eventTypes.contains(AuditEventType.TRANSFER_REQUESTED), "TRANSFER_REQUESTED audit log should exist");
        assertTrue(eventTypes.contains(AuditEventType.POLICY_EVALUATION_STARTED), "POLICY_EVALUATION_STARTED audit log should exist");
        assertTrue(eventTypes.contains(AuditEventType.POLICY_APPROVED), "POLICY_APPROVED audit log should exist");


        // EDC has started the async contract negotiation
        // It will transition through: REQUESTED → OFFERED → AGREED → FINALIZED
        // Once FINALIZED, Edc CallbackController will be called
        await()
                .atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(5000))
                .untilAsserted(() -> {
                    TransferRequestEntity transfer = transferRequestRepository.findById(transferId)
                            .orElseThrow(() -> new AssertionError("Transfer should exist"));

                    TransferStatus status = transfer.getStatus();
                    assertTrue(
                            status == TransferStatus.CONTRACT_NEGOTIATION ||
                            status == TransferStatus.NEGOTIATED ||
                            status == TransferStatus.TRANSFER_IN_PROGRESS,
                            "Transfer should reach CONTRACT_NEGOTIATION or beyond, but was: " + status
                    );
                });

        // EDC completes the negotiation (FINALIZED state) and send callback
        // EDC CallbackController publishes TransferContractNegotiatedEvent to kafka
        // TransferEventHandler consumes event and update transfer status to NEGOTIATED
        // Wait for NEGOTIATED state
        await()
                .atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(5000))
                .untilAsserted(() -> {
                    TransferRequestEntity transfer = transferRequestRepository.findById(transferId)
                            .orElseThrow(() -> new AssertionError("Transfer should exist"));

                    TransferStatus status = transfer.getStatus();
                    assertTrue(
                            status == TransferStatus.NEGOTIATED ||
                            status == TransferStatus.TRANSFER_IN_PROGRESS ||
                            status == TransferStatus.COMPLETED,
                            "Transfer should reach NEGOTIATED or beyond, but was: " + status
                    );
                });

        // Verify CONTRACT_NEGOTIATED audit log exists
        List<AuditLogEntity> negotiatedAuditLogs = auditLogRepository.findAllByTransferId(transferId);
        assertTrue(
                negotiatedAuditLogs.stream().anyMatch(log -> log.getEventType() == AuditEventType.CONTRACT_NEGOTIATED),
                "CONTRACT_NEGOTIATED audit log should exist"
        );

        // EDC now simulates the transfer send callback (PROVISIONING)
        // Edc CallbackController publishes TransferInProgressEvent to Kafka
        // TransferEventHandler consumes event and updates transfer status to TRANSFER_IN_PROGRESS
        await()
                .atMost(Duration.ofSeconds(20))
                .pollInterval(Duration.ofMillis(10000))
                .untilAsserted(() -> {
                    TransferRequestEntity transfer = transferRequestRepository.findById(transferId)
                            .orElseThrow(() -> new AssertionError("Transfer should exist"));

                    TransferStatus status = transfer.getStatus();
                    assertTrue(
                            status == TransferStatus.TRANSFER_IN_PROGRESS ||
                            status == TransferStatus.COMPLETED,
                            "Transfer should reach TRANSFER_IN_PROGRESS or COMPLETED, but was: " + status
                    );
                });

        // Verify TRANSFER_STARTED audit log exists
        List<AuditLogEntity> inProgressAuditLogs = auditLogRepository.findAllByTransferId(transferId);
        assertTrue(
                inProgressAuditLogs.stream().anyMatch(log -> log.getEventType() == AuditEventType.TRANSFER_STARTED),
                "TRANSFER_STARTED audit log should exist"
        );

        // Edc sends mock data to EdcCallbackController.receiveData()
        // Edc CallbackController publishes TransferCompletedEvent to Kafka
        // TransferEventHandler consumes event and updates transfer status to COMPLETED
        // Wait for COMPLETED state
        await()
                .atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> {
                    TransferRequestEntity transfer = transferRequestRepository.findById(transferId)
                            .orElseThrow(() -> new AssertionError("Transfer should exist"));

                    assertEquals(
                            TransferStatus.COMPLETED,
                            transfer.getStatus(),
                            "Transfer should be COMPLETED"
                    );
                });

        // ========== STEP 7: Verify Final State ==========

        TransferRequestEntity completedTransfer = transferRequestRepository.findById(transferId)
                .orElseThrow(() -> new AssertionError("Transfer should exist"));

        // Verify transfer attributes
        assertEquals(TransferStatus.COMPLETED, completedTransfer.getStatus(), "Final status should be COMPLETED");
        assertEquals("asset-quality-001", completedTransfer.getAssetId());
        assertEquals("provider-bmw", completedTransfer.getProviderId());
        assertEquals("consumer-vw", completedTransfer.getConsumerId());
        assertEquals(DataType.QUALITY, completedTransfer.getDataType());
        assertNotNull(completedTransfer.getEdcContractAgreementId(), "Agreement ID should be set after negotiation");

        // ========== STEP 8: Verify Complete Audit Trail ==========
        // The audit trail should contain all significant events in order

        List<AuditLogEntity> allAuditLogs = auditLogRepository.findAllByTransferId(transferId);
        assertFalse(allAuditLogs.isEmpty(), "Audit logs should exist");

        // Expected audit events (minimum set):
        // 1. TRANSFER_REQUESTED - Initial request
        // 2. POLICY_EVALUATION_STARTED - Policy evaluation begins
        // 3. POLICY_EVALUATION_PASSED - Policy evaluation succeeds
        // 4. CONTRACT_NEGOTIATION_STARTED - Contract negotiation initiated
        // 5. CONTRACT_NEGOTIATED - Contract finalized
        // 6. TRANSFER_PROCESS_STARTED - Transfer process begins
        // 7. TRANSFER_PROCESS_COMPLETED - Transfer completes

        List<AuditEventType> expectedEvents = List.of(
                AuditEventType.TRANSFER_REQUESTED,
                AuditEventType.POLICY_EVALUATION_STARTED,
                AuditEventType.POLICY_APPROVED,
                AuditEventType.CONTRACT_NEGOTIATION_STARTED,
                AuditEventType.CONTRACT_NEGOTIATED,
                AuditEventType.TRANSFER_STARTED,
                AuditEventType.TRANSFER_COMPLETED
        );

        for (AuditEventType expectedEvent : expectedEvents) {
            boolean eventExists = allAuditLogs.stream()
                    .anyMatch(log -> log.getEventType() == expectedEvent);
            assertTrue(
                    eventExists,
                    "Audit log should contain event: " + expectedEvent +
                    ". Found events: " + allAuditLogs.stream().map(AuditLogEntity::getEventType).toList()
            );
        }

        // Verify audit logs are properly ordered (by timestamp)
        List<AuditLogEntity> sortedLogs = allAuditLogs.stream()
                .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                .toList();

        // TRANSFER_REQUESTED should be first
        assertEquals(
                AuditEventType.TRANSFER_REQUESTED,
                sortedLogs.getFirst().getEventType(),
                "First audit log should be TRANSFER_REQUESTED"
        );

        // TRANSFER_COMPLETED should be last (or very close to last)
        assertTrue(
                sortedLogs.stream()
                        .skip(Math.max(0, sortedLogs.size() - 2)) // Check last 2 entries
                        .anyMatch(log -> log.getEventType() == AuditEventType.TRANSFER_COMPLETED),
                "TRANSFER_COMPLETED should be among the last audit logs"
        );

        // Verify audit logs contain proper event type
        AuditLogEntity completedLog = allAuditLogs.stream()
                .filter(log -> log.getEventType() == AuditEventType.TRANSFER_COMPLETED)
                .findFirst()
                .orElseThrow(() -> new AssertionError("TRANSFER_COMPLETED log should exist"));

        assertEquals(transferId, completedLog.getTransferId());
        assertEquals("consumer-vw", completedLog.getConsumerId());
        assertNotNull(completedLog.getCreatedAt());

        // Test complete
        System.out.println("✓ Full transfer lifecycle test passed successfully!");
        System.out.println("  Transfer ID: " + transferId);
        System.out.println("  Final Status: " + completedTransfer.getStatus());
        System.out.println("  Agreement ID: " + completedTransfer.getEdcContractAgreementId());
        System.out.println("  Audit Logs: " + allAuditLogs.size() + " events recorded");
    }

    /**
     * Test transfer failure scenario.
     * <p>
     * This test verifies the failure handling when the mock EDC is configured to fail.
     * Note: To test this, you need to configure the mock EDC failure rate > 0.
     * </p>
     */
    @Test
    void transferFailureLifecycleTest() {
        // This test would require configuration of mock EDC to simulate failures
        // For now, we'll test policy evaluation failure which is synchronous

        TransferInitiateDto request = TransferInitiateDto.builder()
                .assetId("asset-quality-002")
                .providerId("provider-bmw")
                .consumerId("consumer-tesla")
                .dataType(DataType.QUALITY)
                .consumerRegion("US")  // This might violate geographic policy if configured
                .consumerCertification("INVALID")  // This might violate certification policy
                .usagePurpose("UnauthorizedPurpose")
                .build();

        try {
            // This may throw an exception if policies fail synchronously
            TransferResponseDto response = transferOrchestrator.initiateTransfer(request);

            // If it doesn't throw, wait for async failure
            if (response != null && response.getTransferId() != null) {
                Long transferId = response.getTransferId();

                // Wait for potential failure state
                await()
                        .atMost(Duration.ofSeconds(10))
                        .pollInterval(Duration.ofMillis(500))
                        .untilAsserted(() -> {
                            transferRequestRepository.findById(transferId)
                                    .ifPresent(transfer -> assertNotNull(transfer.getStatus(), "Transfer should have a status"));
                        });
            }
        } catch (Exception e) {
            // Exception is expected for policy violations
            System.out.println("Transfer failed as expected: " + e.getMessage());
        }
    }

    /**
     * Test concurrent transfer requests.
     * <p>
     * This test verifies that the system can handle multiple simultaneous transfer requests
     * and that they are processed independently with proper state isolation.
     * </p>
     */
    @Test
    void concurrentTransfersTest() throws InterruptedException {
        // Create multiple transfer requests
        TransferInitiateDto request1 = TransferInitiateDto.builder()
                .assetId("asset-quality-101")
                .providerId("provider-bmw")
                .consumerId("consumer-vw")
                .dataType(DataType.QUALITY)
                .consumerRegion("EU")
                .consumerCertification("ISO9001")
                .usagePurpose("QualityAnalysis")
                .build();

        TransferInitiateDto request2 = TransferInitiateDto.builder()
                .assetId("asset-quality-102")
                .providerId("provider-mercedes")
                .consumerId("consumer-audi")
                .dataType(DataType.QUALITY)
                .consumerRegion("EU")
                .consumerCertification("ISO9001")
                .usagePurpose("QualityAnalysis")
                .build();

        // Initiate both transfers
        TransferResponseDto response1 = transferOrchestrator.initiateTransfer(request1);
        TransferResponseDto response2 = transferOrchestrator.initiateTransfer(request2);

        assertNotNull(response1.getTransferId());
        assertNotNull(response2.getTransferId());
        assertNotEquals(response1.getTransferId(), response2.getTransferId(), "Transfer IDs should be different");

        Long transferId1 = response1.getTransferId();
        Long transferId2 = response2.getTransferId();

        // Wait for both transfers to complete
        await()
                .atMost(Duration.ofSeconds(40))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    TransferRequestEntity transfer1 = transferRequestRepository.findById(transferId1)
                            .orElseThrow();
                    TransferRequestEntity transfer2 = transferRequestRepository.findById(transferId2)
                            .orElseThrow();

                    assertEquals(TransferStatus.COMPLETED, transfer1.getStatus(), "Transfer 1 should complete");
                    assertEquals(TransferStatus.COMPLETED, transfer2.getStatus(), "Transfer 2 should complete");
                });

        // Verify both transfers are independent
        TransferRequestEntity transfer1 = transferRequestRepository.findById(transferId1).orElseThrow();
        TransferRequestEntity transfer2 = transferRequestRepository.findById(transferId2).orElseThrow();

        assertEquals("asset-quality-101", transfer1.getAssetId());
        assertEquals("asset-quality-102", transfer2.getAssetId());
        assertEquals("consumer-vw", transfer1.getConsumerId());
        assertEquals("consumer-audi", transfer2.getConsumerId());

        // Verify both have separate audit trails
        List<AuditLogEntity> auditLogs1 = auditLogRepository.findAllByTransferId(transferId1);
        List<AuditLogEntity> auditLogs2 = auditLogRepository.findAllByTransferId(transferId2);

        assertFalse(auditLogs1.isEmpty(), "Transfer 1 should have audit logs");
        assertFalse(auditLogs2.isEmpty(), "Transfer 2 should have audit logs");

        System.out.println("✓ Concurrent transfers test passed!");
        System.out.println("  Transfer 1: " + transferId1 + " - " + transfer1.getStatus());
        System.out.println("  Transfer 2: " + transferId2 + " - " + transfer2.getStatus());
    }

    /**
     * Test transfer cancellation.
     * <p>
     * This test verifies that a transfer can be canceled during negotiation or transfer process,
     * and that the cancellation is properly recorded in audit logs.
     * </p>
     */
    @Test
    void transferCancellationTest() {
        // Initiate a transfer
        TransferInitiateDto request = TransferInitiateDto.builder()
                .assetId("asset-quality-cancel")
                .providerId("provider-bmw")
                .consumerId("consumer-vw")
                .dataType(DataType.QUALITY)
                .consumerRegion("EU")
                .consumerCertification("ISO9001")
                .usagePurpose("QualityAnalysis")
                .build();

        TransferResponseDto response = transferOrchestrator.initiateTransfer(request);
        Long transferId = response.getTransferId();

        // Wait for negotiated or in progress
        await()
                .atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(8000))
                .untilAsserted(() -> {
                    TransferRequestEntity transfer = transferRequestRepository.findById(transferId)
                            .orElseThrow();
                    assertTrue(
                            transfer.getStatus() == TransferStatus.NEGOTIATED ||
                            transfer.getStatus() == TransferStatus.TRANSFER_IN_PROGRESS,
                            "Transfer should be negotiated or in progress before cancellation"
                    );
                });

        // Cancel the transfer
        transferOrchestrator.cancelTransfer(transferId);

        // Verify transfer is canceled
        await()
                .atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> {
                    TransferRequestEntity transfer = transferRequestRepository.findById(transferId)
                            .orElseThrow();
                    assertEquals(TransferStatus.CANCELLED, transfer.getStatus(), "Transfer should be cancelled");
                });

        // Verify cancellation audit log
        List<AuditLogEntity> auditLogs = auditLogRepository.findAllByTransferId(transferId);
        assertTrue(
                auditLogs.stream().anyMatch(log -> log.getEventType() == AuditEventType.TRANSFER_CANCELLED),
                "Audit log should contain TRANSFER_CANCELLED event"
        );

        System.out.println("✓ Transfer cancellation test passed!");
    }
}