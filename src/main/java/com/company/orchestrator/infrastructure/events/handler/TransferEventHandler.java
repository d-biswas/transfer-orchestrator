package com.company.orchestrator.infrastructure.events.handler;

import com.company.orchestrator.audit.service.AuditService;
import com.company.orchestrator.domain.model.TransferStatus;
import com.company.orchestrator.domain.service.TransferStateService;
import com.company.orchestrator.infrastructure.events.model.TransferCompletedEvent;
import com.company.orchestrator.infrastructure.events.model.TransferContractNegotiatedEvent;
import com.company.orchestrator.infrastructure.events.model.TransferFailedEvent;
import com.company.orchestrator.infrastructure.events.model.TransferInProgressEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.errors.RetriableException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

/**
 * Kafka listener responsible for consuming transfer lifecycle events
 * and applying corresponding state transitions.
 *
 * <p>This handler processes events emitted from EDC callbacks and internal
 * publishers, updates {@link TransferStatus}, and records audit information.</p>
 *
 * <p>Events are processed asynchronously with manual acknowledgment,
 * bounded retries for retriable failures, and Dead Letter Queue (DLQ)
 * forwarding for non-retriable errors.</p>
 *
 * <p>Business rules and state validation are delegated to
 * {@link TransferStateService}.</p>
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class TransferEventHandler {

    private final TransferStateService transferStateService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;
    private final KafkaDlqHandler dlqHandler;

    private static final String KAFKA_EVENT_ACTOR = "KAFKA_EVENT";

    /**
     * Handle transfer contact negotiated events
     */
    @KafkaListener(topics = "#{@kafkaProperties.topics['contact-negotiated'].name}")
    @Retryable(
            retryFor = RetriableException.class,
            backoff = @Backoff(delayExpression = "#{@kafkaProperties.consumer.retry.backoffDelay}"),
            maxAttemptsExpression = "#{@kafkaProperties.consumer.retry.maxAttempts}"
    )
    public void handleContractNegotiated(ConsumerRecord<String, String> message, Acknowledgment acknowledgment) {
        try {
            String value = message.value();
            TransferContractNegotiatedEvent event = objectMapper.readValue(value, TransferContractNegotiatedEvent.class);
            log.info("Received TransferInProgressEvent: transferId={}, agreementId={}, correlationId={}",
                    event.getTransferId(), event.getAgreementId(), event.getCorrelationId());
            processContactNegotiated(event);
            acknowledgment.acknowledge();
            log.info("Successfully processed TransferContactNegotiatedEvent: transferId={}", event.getTransferId());

        } catch (RetriableException ex) {
            log.error("Retriable exception while processing message: key={}", message.key(), ex);
            throw ex;
        } catch (Exception ex) {
            log.error("Non-retriable error processing negotiated: key={}", message.key(), ex);
            dlqHandler.sendToDeadLetter(message, ex, acknowledgment);
        }
    }

    /**
     * Handle transfer in progress events
     */
    @KafkaListener(topics = "#{@kafkaProperties.topics['transfer-in-progress'].name}")
    @Retryable(
            retryFor = RetriableException.class,
            backoff = @Backoff(delayExpression = "#{@kafkaProperties.consumer.retry.backoffDelay}"),
            maxAttemptsExpression = "#{@kafkaProperties.consumer.retry.maxAttempts}"
    )
    public void handleTransferInProgress(ConsumerRecord<String, String> message, Acknowledgment acknowledgment) {
        try {
            String value = message.value();
            TransferInProgressEvent event = objectMapper.readValue(value, TransferInProgressEvent.class);
            log.info("Received TransferInProgressEvent: transferId={}, edcTransferProcessId={}, edcState={}",
                    event.getTransferId(), event.getEdcTransferProcessId(), event.getEdcState());
            processTransferInProgress(event);
            acknowledgment.acknowledge();
            log.info("Successfully processed TransferInProgressEvent: transferId={}", event.getTransferId());

        } catch (RetriableException ex) {
            log.error("Retriable exception while processing message: key={}", message.key(), ex);
            throw ex;
        } catch (Exception ex) {
            log.error("Non-retriable error processing transfer in progress: key={}", message.key(), ex);
            dlqHandler.sendToDeadLetter(message, ex, acknowledgment);
        }
    }

    /**
     * Handle transfer completed events
     */
    @KafkaListener(topics = "#{@kafkaProperties.topics['transfer-completed'].name}")
    @Retryable(
            retryFor = RetriableException.class,
            backoff = @Backoff(delayExpression = "#{@kafkaProperties.consumer.retry.backoffDelay}"),
            maxAttemptsExpression = "#{@kafkaProperties.consumer.retry.maxAttempts}"
    )
    public void handleTransferCompleted(ConsumerRecord<String, String> message, Acknowledgment acknowledgment) {
        try {
            String value = message.value();
            TransferCompletedEvent event = objectMapper.readValue(value, TransferCompletedEvent.class);
            log.info("Received TransferCompletedEvent: transferId={}, bytesTransferred={}, edcTransferProcessId={}",
                    event.getTransferId(), event.getBytesTransferred(), event.getEdcTransferProcessId());
            processTransferCompleted(event);
            acknowledgment.acknowledge();
            log.info("Successfully processed TransferCompletedEvent: transferId={}", event.getTransferId());

        } catch (RetriableException ex) {
            log.error("Retriable exception while processing message: key={}", message.key(), ex);
            throw ex;
        } catch (Exception ex) {
            log.error("Non-retriable error processing transfer completed: key={}", message.key(), ex);
            dlqHandler.sendToDeadLetter(message, ex, acknowledgment);
        }
    }

    /**
     * Handle transfer failed events
     */
    @KafkaListener(topics = "#{@kafkaProperties.topics['transfer-failed'].name}")
    @Retryable(
            retryFor = RetriableException.class,
            backoff = @Backoff(delayExpression = "#{@kafkaProperties.consumer.retry.backoffDelay}"),
            maxAttemptsExpression = "#{@kafkaProperties.consumer.retry.maxAttempts}"
    )
    public void handleTransferFailed(ConsumerRecord<String, String> message, Acknowledgment acknowledgment) {
        try {
            TransferFailedEvent event = objectMapper.readValue(message.value(), TransferFailedEvent.class);
            log.info("Received TransferFailedEvent: transferId={}, errorMessage={}, errorCode={}",
                    event.getTransferId(), event.getErrorMessage(), event.getErrorCode());
            processTransferFailed(event);
            acknowledgment.acknowledge();
            log.info("Successfully processed TransferFailedEvent: transferId={}", event.getTransferId());

        } catch (RetriableException ex) {
            log.error("Retriable exception while processing message: key={}", message.key(), ex);
            throw ex;
        } catch (Exception ex) {
            log.error("Non-retriable error processing transfer failed: key={}", message.key(), ex);
            dlqHandler.sendToDeadLetter(message, ex, acknowledgment);
        }
    }

    /**
     * Process transfer negotiated event
     * Updates transfer state to CONTRACT_NEGOTIATED
     */
    private void processContactNegotiated(TransferContractNegotiatedEvent event) {
        log.debug("Processing contact negotiated: transferId={}, agreementId={}, correlationId={}",
                event.getTransferId(), event.getAgreementId(), event.getCorrelationId());

        try {
            Long transferId = event.getTransferId();
            TransferStatus currentStatus = transferStateService.getStatus(transferId);

            // Only update if not already in NEGOTIATED or terminal state
            if (currentStatus != TransferStatus.NEGOTIATED &&
                    currentStatus != TransferStatus.COMPLETED &&
                    currentStatus != TransferStatus.FAILED &&
                    currentStatus != TransferStatus.CANCELLED) {

                transferStateService.updateEdcContractAgreementId(transferId, event.getAgreementId());
                transferStateService.updateState(transferId, TransferStatus.NEGOTIATED, KAFKA_EVENT_ACTOR);
                auditService.logStateTransition(transferId.toString(), currentStatus, TransferStatus.NEGOTIATED);

                log.info("Transfer state updated to NEGOTIATED: transferId={}, agreementId={}, correlationId={}",
                        transferId, event.getAgreementId(), event.getCorrelationId());
            } else {
                log.debug("Transfer already negotiated or terminal state: transferId={}, currentStatus={}",
                        transferId, currentStatus);
            }
        } catch (Exception ex) {
            log.error("Error in processContactNegotiated: transferId={}, error={}",
                    event.getTransferId(), ex.getMessage(), ex);
            throw ex;
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

            // Only update if not already in TRANSFER_IN_PROGRESS or terminal state
            if (currentStatus != TransferStatus.TRANSFER_IN_PROGRESS &&
                    currentStatus != TransferStatus.COMPLETED &&
                    currentStatus != TransferStatus.FAILED &&
                    currentStatus != TransferStatus.CANCELLED) {

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
            // Only update if not already in COMPLETED or terminal state
            if (currentStatus != TransferStatus.COMPLETED &&
                    currentStatus != TransferStatus.CANCELLED) {
                transferStateService.updateState(transferId, TransferStatus.COMPLETED, KAFKA_EVENT_ACTOR);
                auditService.logStateTransition(transferId.toString(), currentStatus, TransferStatus.COMPLETED);

                log.info("Transfer completed successfully: transferId={}, bytesTransferred={}, edcProcessId={}",
                        transferId, event.getBytesTransferred(), event.getEdcTransferProcessId());
            } else {
                log.debug("Transfer already completed or terminal state: transferId={}, currentStatus={}",
                        transferId, currentStatus);
            }
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
            if (currentStatus != TransferStatus.FAILED &&
                    currentStatus != TransferStatus.CANCELLED) {
                transferStateService.updateState(transferId, TransferStatus.FAILED, KAFKA_EVENT_ACTOR);
                auditService.logStateTransition(transferId.toString(), currentStatus, TransferStatus.FAILED);
                log.error("Transfer failed: transferId={}, errorCode={}, errorMessage={}, previousState={}",
                        transferId, event.getErrorCode(), event.getErrorMessage(), currentStatus);
            } else {
                log.debug("Transfer already failed or terminal state: transferId={}, currentStatus={}",
                        transferId, currentStatus);
            }
        } catch (Exception ex) {
            log.error("Error in processTransferFailed: transferId={}, error={}",
                    event.getTransferId(), ex.getMessage(), ex);
            throw ex;
        }
    }
}