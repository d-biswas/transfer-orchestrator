package com.company.orchestrator.infrastructure.events.handler;

import com.company.orchestrator.audit.service.AuditService;
import com.company.orchestrator.domain.model.TransferStatus;
import com.company.orchestrator.domain.service.TransferStateService;
import com.company.orchestrator.infrastructure.events.model.TransferCompletedEvent;
import com.company.orchestrator.infrastructure.events.model.TransferFailedEvent;
import com.company.orchestrator.infrastructure.events.model.TransferInProgressEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

/**
 * Kafka event handler for transfer-related events
 * Handles all events published by TransferEventPublisher with retry and DLQ support
 * <p>
 * This handler processes events asynchronously to:
 * - Update transfer states in response to EDC callbacks
 * - Maintain audit trails
 * - Handle notifications and analytics
 * - Decouple EDC callback processing from business logic
 * <p>
 * Note: @KafkaListener annotations use SpEL expressions to read topic names from KafkaProperties
 * at startup time. This is the standard Spring approach for annotation-based configuration.
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class TransferEventHandler {

    private final TransferStateService transferStateService;
    private final AuditService auditService;

    private static final String KAFKA_EVENT_ACTOR = "KAFKA_EVENT";

    /**
     * Handle transfer in progress events
     */
    @KafkaListener(
            topics = "${spring.kafka.topics.transfer-in-progress.name}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleTransferInProgress(
            @Payload TransferInProgressEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        log.info("Received TransferInProgressEvent: transferId={}, edcTransferProcessId={}, edcState={}, topic={}, partition={}, offset={}",
                event.getTransferId(), event.getEdcTransferProcessId(), event.getEdcState(), topic, partition, offset);

        try {
            processTransferInProgress(event);
            acknowledgment.acknowledge();
            log.info("Successfully processed TransferInProgressEvent: transferId={}", event.getTransferId());

        } catch (Exception ex) {
            log.error("Error processing TransferInProgressEvent: transferId={}, error={}",
                    event.getTransferId(), ex.getMessage(), ex);
            // Do not acknowledge - will trigger retry
            throw new RuntimeException("Failed to process TransferInProgressEvent", ex);
        }
    }

    /**
     * Handle transfer completed events
     */
    @KafkaListener(
            topics = "${spring.kafka.topics.transfer-completed.name}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleTransferCompleted(
            @Payload TransferCompletedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        log.info("Received TransferCompletedEvent: transferId={}, bytesTransferred={}, edcTransferProcessId={}, topic={}, partition={}, offset={}",
                event.getTransferId(), event.getBytesTransferred(), event.getEdcTransferProcessId(), topic, partition, offset);

        try {
            processTransferCompleted(event);
            acknowledgment.acknowledge();
            log.info("Successfully processed TransferCompletedEvent: transferId={}", event.getTransferId());

        } catch (Exception ex) {
            log.error("Error processing TransferCompletedEvent: transferId={}, error={}",
                    event.getTransferId(), ex.getMessage(), ex);
            throw new RuntimeException("Failed to process TransferCompletedEvent", ex);
        }
    }

    /**
     * Handle transfer failed events
     */
    @KafkaListener(
            topics = "${spring.kafka.topics.transfer-failed.name}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleTransferFailed(
            @Payload TransferFailedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        log.info("Received TransferFailedEvent: transferId={}, errorMessage={}, errorCode={}, topic={}, partition={}, offset={}",
                event.getTransferId(), event.getErrorMessage(), event.getErrorCode(), topic, partition, offset);

        try {
            processTransferFailed(event);
            acknowledgment.acknowledge();
            log.info("Successfully processed TransferFailedEvent: transferId={}", event.getTransferId());

        } catch (Exception ex) {
            log.error("Error processing TransferFailedEvent: transferId={}, error={}",
                    event.getTransferId(), ex.getMessage(), ex);
            throw new RuntimeException("Failed to process TransferFailedEvent", ex);
        }
    }

    /**
     * Process transfer in progress event
     * Updates transfer state to TRANSFER_IN_PROGRESS
     */
    private void processTransferInProgress(TransferInProgressEvent event) {
        log.debug("Processing transfer in progress: transferId={}, edcTransferProcessId={}, edcState={}",
                event.getTransferId(), event.getEdcTransferProcessId(), event.getEdcState());

        try {
            Long transferId = event.getTransferId();
            TransferStatus currentStatus = transferStateService.getStatus(transferId);

            // Only update if not already in TRANSFER_IN_PROGRESS or beyond
            if (currentStatus != TransferStatus.TRANSFER_IN_PROGRESS &&
                    currentStatus != TransferStatus.COMPLETED &&
                    currentStatus != TransferStatus.FAILED) {

                transferStateService.updateState(transferId, TransferStatus.TRANSFER_IN_PROGRESS, KAFKA_EVENT_ACTOR);
                auditService.logStateTransition(transferId.toString(), currentStatus, TransferStatus.TRANSFER_IN_PROGRESS);

                log.info("Transfer state updated to IN_PROGRESS: transferId={}, edcState={}, edcProcessId={}",
                        transferId, event.getEdcState(), event.getEdcTransferProcessId());
            } else {
                log.debug("Transfer already in progress or terminal state: transferId={}, currentStatus={}",
                        transferId, currentStatus);
            }

        } catch (Exception ex) {
            log.error("Error in processTransferInProgress: transferId={}, error={}",
                    event.getTransferId(), ex.getMessage(), ex);
            throw ex;
        }
    }

    /**
     * Process transfer completed event
     * Updates transfer state to COMPLETED and handles post-completion actions
     */
    private void processTransferCompleted(TransferCompletedEvent event) {
        log.debug("Processing transfer completed: transferId={}, bytesTransferred={}, edcTransferProcessId={}",
                event.getTransferId(), event.getBytesTransferred(), event.getEdcTransferProcessId());

        try {
            Long transferId = event.getTransferId();
            TransferStatus currentStatus = transferStateService.getStatus(transferId);
            transferStateService.updateState(transferId, TransferStatus.COMPLETED, KAFKA_EVENT_ACTOR);
            auditService.logStateTransition(transferId.toString(), currentStatus, TransferStatus.COMPLETED);

            log.info("Transfer completed successfully: transferId={}, bytesTransferred={}, edcProcessId={}",
                    transferId, event.getBytesTransferred(), event.getEdcTransferProcessId());

        } catch (Exception ex) {
            log.error("Error in processTransferCompleted: transferId={}, error={}",
                    event.getTransferId(), ex.getMessage(), ex);
            throw ex;
        }
    }

    /**
     * Process transfer failed event
     * Updates transfer state to FAILED and handles failure scenarios
     */
    protected void processTransferFailed(TransferFailedEvent event) {
        log.debug("Processing transfer failed: transferId={}, errorMessage={}, errorCode={}",
                event.getTransferId(), event.getErrorMessage(), event.getErrorCode());

        try {
            Long transferId = event.getTransferId();
            TransferStatus currentStatus = transferStateService.getStatus(transferId);
            transferStateService.updateState(transferId, TransferStatus.FAILED, KAFKA_EVENT_ACTOR);
            auditService.logStateTransition(transferId.toString(), currentStatus, TransferStatus.FAILED);
            log.error("Transfer failed: transferId={}, errorCode={}, errorMessage={}, previousState={}",
                    transferId, event.getErrorCode(), event.getErrorMessage(), currentStatus);
        } catch (Exception ex) {
            log.error("Error in processTransferFailed: transferId={}, error={}",
                    event.getTransferId(), ex.getMessage(), ex);
            throw ex;
        }
    }
}