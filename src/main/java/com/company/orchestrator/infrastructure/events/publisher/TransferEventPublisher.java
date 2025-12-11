package com.company.orchestrator.infrastructure.events.publisher;

import com.company.orchestrator.infrastructure.events.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

/**
 * Publisher for transfer-related Kafka events
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransferEventPublisher {

    private final KafkaTemplate<String, BaseTransferEvent> kafkaTemplate;

    private static final String SYSTEM_ACTOR = "SYSTEM";

    /**
     * Publish transfer requested event
     */
    public void publishTransferRequested(Long transferId, String consumerId, String providerId,
                                         String assetId, String dataType) {
        TransferRequestedEvent event = TransferRequestedEvent.builder()
                .transferId(transferId)
                .consumerId(consumerId)
                .providerId(providerId)
                .assetId(assetId)
                .dataType(dataType)
                .timestamp(Instant.now())
                .actor(SYSTEM_ACTOR)
                .build();

        publish("transfer.requested", event);
    }

    /**
     * Publish policy approved event
     */
    public void publishPolicyApproved(Long transferId, String policyType) {
        PolicyApprovedEvent event = PolicyApprovedEvent.builder()
                .transferId(transferId)
                .policyType(policyType)
                .timestamp(Instant.now())
                .actor(SYSTEM_ACTOR)
                .build();

        publish("policy.approved", event);
    }

    /**
     * Publish policy rejected event
     */
    public void publishPolicyRejected(Long transferId, String policyType, String violationReason) {
        PolicyRejectedEvent event = PolicyRejectedEvent.builder()
                .transferId(transferId)
                .policyType(policyType)
                .violationReason(violationReason)
                .timestamp(Instant.now())
                .actor(SYSTEM_ACTOR)
                .build();

        publish("policy.rejected", event);
    }

    /**
     * Publish transfer negotiation started event
     */
    public void publishNegotiationStarted(Long transferId, String providerId, String assetId) {
        TransferNegotiationStartedEvent event = TransferNegotiationStartedEvent.builder()
                .transferId(transferId)
                .providerId(providerId)
                .assetId(assetId)
                .timestamp(Instant.now())
                .actor(SYSTEM_ACTOR)
                .build();

        publish("transfer.negotiation.started", event);
    }

    /**
     * Publish transfer completed event
     */
    public void publishTransferCompleted(Long transferId, Long bytesTransferred, String edcTransferProcessId) {
        TransferCompletedEvent event = TransferCompletedEvent.builder()
                .transferId(transferId)
                .bytesTransferred(bytesTransferred)
                .edcTransferProcessId(edcTransferProcessId)
                .timestamp(Instant.now())
                .actor(SYSTEM_ACTOR)
                .build();

        publish("transfer.completed", event);
    }

    /**
     * Publish transfer failed event
     */
    public void publishTransferFailed(Long transferId, String errorMessage, String errorCode) {
        TransferFailedEvent event = TransferFailedEvent.builder()
                .transferId(transferId)
                .errorMessage(errorMessage)
                .errorCode(errorCode)
                .timestamp(Instant.now())
                .actor(SYSTEM_ACTOR)
                .build();

        publish("transfer.failed", event);
    }

    /**
     * Generic publish method with error handling
     */
    private void publish(String topic, BaseTransferEvent event) {
        log.info("Publishing event to topic {}: transferId={}, eventType={}",
                topic, event.getTransferId(), event.getEventType());

        CompletableFuture<Void> future = kafkaTemplate.send(topic, event.getTransferId().toString(), event)
                .thenAccept(result -> log.debug("Successfully published event to topic {}: {}",
                        topic, event.getEventType()))
                .exceptionally(ex -> {
                    log.error("Failed to publish event to topic {}: transferId={}, error={}",
                            topic, event.getTransferId(), ex.getMessage(), ex);
                    return null;
                });
    }
}
