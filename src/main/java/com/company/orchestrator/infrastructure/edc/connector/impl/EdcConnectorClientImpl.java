package com.company.orchestrator.infrastructure.edc.connector.impl;

import com.company.orchestrator.infrastructure.edc.connector.EdcConnectorClient;
import com.company.orchestrator.infrastructure.edc.model.*;
import com.company.orchestrator.infrastructure.props.EdcProperties;
import jakarta.annotation.PreDestroy;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Mock implementation of EDC Connector Client
 * Simulates realistic EDC behavior with async state transitions and callbacks
 */
@Log4j2
@Service
@Primary
@RequiredArgsConstructor
public class EdcConnectorClientImpl implements EdcConnectorClient {

    private final RestTemplate mockEdcRestTemplate;
    private final EdcProperties edcProperties;
    private final ScheduledExecutorService mockEdcScheduler;

    // In-memory storage for mock state
    private final Map<String, NegotiationState> negotiations = new ConcurrentHashMap<>();
    private final Map<String, TransferState> transfers = new ConcurrentHashMap<>();

    /**
     * Inner class to track contract negotiation state
     */
    @Data
    private static class NegotiationState {
        String negotiationId;
        String currentState;
        String agreementId;
        ContractOffer offer;
        Instant createdAt;
    }

    /**
     * Inner class to track transfer process state
     */
    @Data
    private static class TransferState {
        String transferProcessId;
        String currentState;
        String agreementId;
        TransferRequest request;
        Instant createdAt;
    }

    @Override
    public ContractNegotiationResult negotiateContract(ContractOffer offer) {
        String negotiationId = "negotiation-" + UUID.randomUUID();

        log.info("Starting contract negotiation: negotiationId={}, assetId={}, providerId={}",
                negotiationId, offer.getAssetId(), offer.getProviderId());

        // Store negotiation state
        NegotiationState state = new NegotiationState();
        state.setNegotiationId(negotiationId);
        state.setCurrentState("REQUESTED");
        state.setOffer(offer);
        state.setCreatedAt(Instant.now());
        negotiations.put(negotiationId, state);

        // Schedule async callbacks
        scheduleContractNegotiationCallbacks(negotiationId, offer);

        // Return immediate response (async pattern)
        return ContractNegotiationResult.success(negotiationId, null, "REQUESTED");
    }

    @Override
    public TransferProcessResult initiateTransfer(String agreementId, TransferRequest request) {
        String transferProcessId = "transfer-" + UUID.randomUUID();

        log.info("Initiating transfer: transferProcessId={}, agreementId={}, assetId={}",
                transferProcessId, agreementId, request.getAssetId());

        // Store transfer state
        TransferState state = new TransferState();
        state.setTransferProcessId(transferProcessId);
        state.setCurrentState("INITIAL");
        state.setAgreementId(agreementId);
        state.setRequest(request);
        state.setCreatedAt(Instant.now());
        transfers.put(transferProcessId, state);

        // Schedule async callbacks
        scheduleTransferCallbacks(transferProcessId, request);

        // Return immediate response (async pattern)
        return TransferProcessResult.success(transferProcessId, "INITIAL");
    }

    @Override
    public TransferProcessState getTransferState(String transferProcessId) {
        log.debug("Getting transfer state: transferProcessId={}", transferProcessId);

        TransferState state = transfers.get(transferProcessId);
        if (state == null) {
            log.warn("Transfer process not found: {}", transferProcessId);
            return TransferProcessState.builder()
                    .transferProcessId(transferProcessId)
                    .state("UNKNOWN")
                    .stateTimestamp(Instant.now().toEpochMilli())
                    .errorDetail("Transfer process not found")
                    .build();
        }

        return TransferProcessState.builder()
                .transferProcessId(transferProcessId)
                .state(state.getCurrentState())
                .stateTimestamp(state.getCreatedAt().toEpochMilli())
                .build();
    }

    @Override
    public void terminateTransfer(String transferProcessId) {
        log.info("Mock: Terminating transfer: transferProcessId={}", transferProcessId);

        TransferState state = transfers.get(transferProcessId);
        if (state == null) {
            log.warn("Mock: Cannot terminate - transfer process not found: {}", transferProcessId);
            throw new RuntimeException("Transfer process not found: " + transferProcessId);
        }

        // Update state
        state.setCurrentState("TERMINATED");

        // Extract correlation ID and send immediate callback
        String correlationId = extractTransferId(state.getRequest().getDestinationUrl());
        String callbackUrl = edcProperties.getCallbackBaseUrl() + "/api/v1/transfers/callback";

        Map<String, Object> payload = buildTransferCallbackPayload(
                transferProcessId, "TERMINATED", correlationId
        );
        payload.put("errorDetail", "Transfer terminated by orchestrator");

        sendCallback(callbackUrl, payload);

        log.info("Transfer terminated successfully: transferProcessId={}", transferProcessId);
    }

    /**
     * Schedule contract negotiation state callbacks
     * States: REQUESTED → OFFERED → AGREED → FINALIZED
     */
    private void scheduleContractNegotiationCallbacks(String negotiationId, ContractOffer offer) {
        String correlationId = extractCorrelationId(offer.getOfferId());
        String callbackUrl = offer.getConsumerCallbackUrl();

        if (callbackUrl == null || callbackUrl.isEmpty()) {
            log.warn("No callback URL provided in ContractOffer, using default");
            callbackUrl = edcProperties.getCallbackBaseUrl() + "/api/v1/transfers/callback";
        }

        log.debug("Scheduling contract negotiation callbacks: negotiationId={}, correlationId={}, callbackUrl={}",
                negotiationId, correlationId, callbackUrl);

        final String finalCallbackUrl = callbackUrl;

        // Schedule: REQUESTED → OFFERED
        long offeredDelay = edcProperties.getNegotiationRequestedToOfferedDelayMs();
        mockEdcScheduler.schedule(() -> {
            try {
                negotiations.get(negotiationId).setCurrentState("OFFERED");
                Map<String, Object> payload = buildNegotiationCallbackPayload(
                        negotiationId, "OFFERED", correlationId, null
                );
                sendCallback(finalCallbackUrl, payload);
                log.info("Contract negotiation state: OFFERED (negotiationId={})", negotiationId);
            } catch (Exception e) {
                log.error("Error sending OFFERED callback", e);
            }
        }, offeredDelay, TimeUnit.MILLISECONDS);

        // Schedule: OFFERED → AGREED
        long agreedDelay = offeredDelay + edcProperties.getNegotiationOfferedToAgreedDelayMs();
        mockEdcScheduler.schedule(() -> {
            try {
                negotiations.get(negotiationId).setCurrentState("AGREED");
                Map<String, Object> payload = buildNegotiationCallbackPayload(
                        negotiationId, "AGREED", correlationId, null
                );
                sendCallback(finalCallbackUrl, payload);
                log.info("Contract negotiation state: AGREED (negotiationId={})", negotiationId);
            } catch (Exception e) {
                log.error("Error sending AGREED callback", e);
            }
        }, agreedDelay, TimeUnit.MILLISECONDS);

        // Schedule: AGREED → FINALIZED (with agreement ID)
        long finalizedDelay = agreedDelay + edcProperties.getNegotiationAgreedToFinalizedDelayMs();
        mockEdcScheduler.schedule(() -> {
            try {
                String agreementId = "agreement-" + UUID.randomUUID();
                NegotiationState state = negotiations.get(negotiationId);
                state.setCurrentState("FINALIZED");
                state.setAgreementId(agreementId);

                Map<String, Object> payload = buildNegotiationCallbackPayload(
                        negotiationId, "FINALIZED", correlationId, agreementId
                );

                sendCallback(finalCallbackUrl, payload);
                log.info("Contract negotiation state: FINALIZED (negotiationId={}, agreementId={})",
                        negotiationId, agreementId);
            } catch (Exception e) {
                log.error("Error sending FINALIZED callback", e);
            }
        }, finalizedDelay, TimeUnit.MILLISECONDS);
    }

    /**
     * Schedule transfer process state callbacks
     * States: INITIAL → PROVISIONING → STARTED → COMPLETED
     */
    private void scheduleTransferCallbacks(String transferProcessId, TransferRequest request) {
        String correlationId = extractTransferId(request.getDestinationUrl());
        String callbackUrl = edcProperties.getCallbackBaseUrl() + "/api/v1/transfers/callback";

        log.debug("Scheduling transfer callbacks: transferProcessId={}, correlationId={}, callbackUrl={}",
                transferProcessId, correlationId, callbackUrl);

        // Schedule: INITIAL → PROVISIONING
        long provisioningDelay = edcProperties.getTransferInitialToProvisioningDelayMs();
        mockEdcScheduler.schedule(() -> {
            try {
                transfers.get(transferProcessId).setCurrentState("PROVISIONING");
                Map<String, Object> payload = buildTransferCallbackPayload(
                        transferProcessId, "PROVISIONING", correlationId
                );
                sendCallback(callbackUrl, payload);
                log.info("Transfer state: PROVISIONING (transferProcessId={})", transferProcessId);
            } catch (Exception e) {
                log.error("Error sending PROVISIONING callback", e);
            }
        }, provisioningDelay, TimeUnit.MILLISECONDS);

        // Schedule: PROVISIONING → STARTED
        long startedDelay = provisioningDelay + edcProperties.getTransferProvisioningToStartedDelayMs();
        mockEdcScheduler.schedule(() -> {
            try {
                transfers.get(transferProcessId).setCurrentState("STARTED");
                Map<String, Object> payload = buildTransferCallbackPayload(
                        transferProcessId, "STARTED", correlationId
                );
                sendCallback(callbackUrl, payload);
                log.info("Transfer state: STARTED (transferProcessId={})", transferProcessId);
            } catch (Exception e) {
                log.error("Error sending STARTED callback", e);
            }
        }, startedDelay, TimeUnit.MILLISECONDS);

        // Schedule: STARTED → COMPLETED (also send mock data)
        long completedDelay = startedDelay + edcProperties.getTransferStartedToCompletedDelayMs();
        mockEdcScheduler.schedule(() -> {
            try {
                // First send mock data to data/receive endpoint
                sendMockData(transferProcessId, request.getDestinationUrl(), correlationId);

                // Then send COMPLETED callback
                transfers.get(transferProcessId).setCurrentState("COMPLETED");
                Map<String, Object> payload = buildTransferCallbackPayload(
                        transferProcessId, "COMPLETED", correlationId
                );
                sendCallback(callbackUrl, payload);
                log.info("Transfer state: COMPLETED (transferProcessId={})", transferProcessId);
            } catch (Exception e) {
                log.error("Error sending COMPLETED callback", e);
            }
        }, completedDelay, TimeUnit.MILLISECONDS);
    }

    /**
     * Send data to the data receive endpoint
     */
    private void sendMockData(String transferProcessId, String destinationUrl, String transferId) {
        try {
            log.info("Sending mock data: transferProcessId={}, destinationUrl={}, transferId={}",
                    transferProcessId, destinationUrl, transferId);

            // Generate data payload
            String mockDataContent = String.format(
                    "{\"message\":\"Data transfer\",\"transferId\":\"%s\",\"transferProcessId\":\"%s\",\"timestamp\":\"%s\",\"dataSize\":1024}",
                    transferId, transferProcessId, Instant.now()
            );
            byte[] dataBytes = mockDataContent.getBytes(StandardCharsets.UTF_8);

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.set("Edc-Transfer-Process-Id", transferProcessId);

            // Create request
            HttpEntity<byte[]> requestEntity = new HttpEntity<>(dataBytes, headers);

            // Send to data/receive endpoint
            mockEdcRestTemplate.postForEntity(destinationUrl, requestEntity, String.class);

            log.info("Successfully sent mock data: transferProcessId={}, bytes={}", transferProcessId, dataBytes.length);
        } catch (Exception e) {
            log.error("Failed to send mock data: transferProcessId={}, error={}", transferProcessId, e.getMessage(), e);
        }
    }

    /**
     * Build callback payload for contract negotiation
     */
    private Map<String, Object> buildNegotiationCallbackPayload(String processId, String state,
                                                                  String correlationId, String agreementId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", processId);
        payload.put("processId", processId);
        payload.put("state", state);
        payload.put("status", state);
        payload.put("correlationId", correlationId);
        payload.put("type", "contract.negotiation." + state.toLowerCase());
        payload.put("eventType", "contract.negotiation." + state.toLowerCase());

        if (agreementId != null) {
            payload.put("agreementId", agreementId);
            payload.put("contractAgreementId", agreementId);
        }

        return payload;
    }

    /**
     * Build callback payload for transfer process
     */
    private Map<String, Object> buildTransferCallbackPayload(String processId, String state, String correlationId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", processId);
        payload.put("transferProcessId", processId);
        payload.put("state", state);
        payload.put("status", state);
        payload.put("correlationId", correlationId);
        payload.put("type", "transfer.process." + state.toLowerCase());
        payload.put("eventType", "transfer.process." + state.toLowerCase());

        return payload;
    }

    /**
     * Send HTTP callback to orchestrator
     */
    private void sendCallback(String callbackUrl, Map<String, Object> payload) {
        try {
            log.debug("Mock: Sending callback to {}: {}", callbackUrl, payload);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            mockEdcRestTemplate.postForEntity(callbackUrl, request, String.class);

            log.debug("Mock: Callback sent successfully");
        } catch (Exception e) {
            log.error("Mock: Failed to send callback to {}: {}", callbackUrl, e.getMessage(), e);
        }
    }

    /**
     * Extract correlation ID from offer ID
     * Format: "offer-{transferId}" → "{transferId}"
     */
    private String extractCorrelationId(String offerId) {
        if (offerId == null) {
            return null;
        }

        // Try to extract transferId from "offer-123" format
        if (offerId.startsWith("offer-")) {
            return offerId.substring(6);
        }

        return offerId;
    }

    /**
     * Extract transfer ID from destination URL
     * Format: "...?transferId=123" → "123"
     */
    private String extractTransferId(String destinationUrl) {
        if (destinationUrl == null) {
            return null;
        }

        try {
            // Extract transferId query parameter
            if (destinationUrl.contains("transferId=")) {
                String[] parts = destinationUrl.split("transferId=");
                if (parts.length > 1) {
                    String value = parts[1].split("&")[0];
                    return value;
                }
            }
        } catch (Exception e) {
            log.warn("Mock: Failed to extract transferId from URL: {}", destinationUrl);
        }

        return null;
    }

    /**
     * Graceful shutdown - cleanup scheduled tasks
     */
    @PreDestroy
    public void shutdown() {
        log.info("Shutting down Mock EDC Connector...");

        mockEdcScheduler.shutdown();
        try {
            if (!mockEdcScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("Forcing shutdown of scheduler");
                mockEdcScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            log.warn("Interrupted during shutdown");
            mockEdcScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("Mock EDC Connector shutdown complete");
    }
}
