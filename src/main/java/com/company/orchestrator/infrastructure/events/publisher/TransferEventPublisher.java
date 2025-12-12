package com.company.orchestrator.infrastructure.events.publisher;

import com.company.orchestrator.infrastructure.events.model.*;
import com.company.orchestrator.infrastructure.props.KafkaProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

/**
 * Publisher for transfer-related Kafka events
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class TransferEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaProperties kafkaProperties;

    private static final String SYSTEM_ACTOR = "SYSTEM";

    /**
     * Publish transfer in progress event
     */
    public void publishTransferInProgress(Long transferId, String edcTransferProcessId, String edcState) {
        TransferInProgressEvent event = TransferInProgressEvent.builder()
                .transferId(transferId)
                .edcTransferProcessId(edcTransferProcessId)
                .edcState(edcState)
                .timestamp(Instant.now())
                .actor(SYSTEM_ACTOR)
                .build();

        String topic = kafkaProperties.getTopics().get("transfer-in-progress").getName();
        publish(topic, event);
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

        String topic = kafkaProperties.getTopics().get("transfer-completed").getName();
        publish(topic, event);
    }

    /**
     * Publish transfer failed event
     */
    public void publishTransferFailed(Long transferId, String errorMessage, TransferFailedErrorCode errorCode) {
        TransferFailedEvent event = TransferFailedEvent.builder()
                .transferId(transferId)
                .errorMessage(errorMessage)
                .errorCode(errorCode.getCode())
                .timestamp(Instant.now())
                .actor(SYSTEM_ACTOR)
                .build();

        String topic = kafkaProperties.getTopics().get("transfer-failed").getName();
        publish(topic, event);
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
