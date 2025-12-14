package com.company.orchestrator.infrastructure.events.handler;

import com.company.orchestrator.infrastructure.props.KafkaProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles routing of failed Kafka messages to a Dead Letter Queue (DLQ)
 * with error metadata and manual offset acknowledgment.
 */
@Log4j2
@Component
@RequiredArgsConstructor
public class KafkaDlqHandler {
    private final KafkaProperties kafkaProperties;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Sends failed message to Dead Letter Queue (DLQ) and acknowledges the original message
     *
     * @param message The original Kafka consumer record that failed
     * @param ex The exception that caused the failure
     * @param ack The acknowledgment to commit the offset
     */
    public void sendToDeadLetter(ConsumerRecord<String, String> message, Exception ex, Acknowledgment ack) {
        sendToDeadLetter(message, ex, ack, null);
    }

    /**
     * Sends failed message to Dead Letter Queue with additional context
     *
     * @param message The original Kafka consumer record that failed
     * @param ex The exception that caused the failure
     * @param ack The acknowledgment to commit the offset
     * @param additionalContext Additional context to include in the DLQ message
     */
    public void sendToDeadLetter(ConsumerRecord<String, String> message, Exception ex,
                                   Acknowledgment ack, Map<String, Object> additionalContext) {
        String deadLetterTopic = kafkaProperties.getTopics().get("dlq").getName();

        Map<String, Object> dlqMessage = new HashMap<>();
        dlqMessage.put("Key", message.key());
        dlqMessage.put("Value", message.value());
        dlqMessage.put("Error", ex.getMessage());
        dlqMessage.put("ErrorType", ex.getClass().getName());
        dlqMessage.put("StackTrace", getStackTraceAsString(ex));
        dlqMessage.put("Topic", message.topic());
        dlqMessage.put("Partition", message.partition());
        dlqMessage.put("Offset", message.offset());
        dlqMessage.put("Timestamp", System.currentTimeMillis());

        // Add additional context
        if (additionalContext != null && !additionalContext.isEmpty()) {
            dlqMessage.put("Context", additionalContext);
        }

        try {
            kafkaTemplate.send(deadLetterTopic, message.key() != null ? message.key() : "", dlqMessage);
            log.info("Message sent to DLQ with context: topic={} key={} originalTopic={} error={}",
                    deadLetterTopic, message.key(), message.topic(), ex.getMessage());
        } catch (Exception dlqException) {
            log.error("Failed to send message to DLQ: topic={} key={}",
                    deadLetterTopic, message.key(), dlqException);
        } finally {
            ack.acknowledge();
        }
    }

    /**
     * Converts an exception stack trace into a truncated string for DLQ payloads.
     * */
    private String getStackTraceAsString(Exception ex) {
        if (ex == null) return "";

        StringBuilder sb = new StringBuilder();
        sb.append(ex.getClass().getName()).append(": ").append(ex.getMessage()).append("\n");

        for (StackTraceElement element : ex.getStackTrace()) {
            sb.append("\tat ").append(element.toString()).append("\n");
            // Limit stack trace to first 10 elements to avoid huge DLQ messages
            if (sb.length() > 2000) {
                sb.append("\t... (truncated)");
                break;
            }
        }
        return sb.toString();
    }
}