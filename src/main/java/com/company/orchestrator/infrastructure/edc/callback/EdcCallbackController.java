package com.company.orchestrator.infrastructure.edc.callback;

import com.company.orchestrator.api.constants.ApiConstants;
import com.company.orchestrator.audit.service.AuditService;
import com.company.orchestrator.domain.model.TransferStatus;
import com.company.orchestrator.domain.service.TransferStateService;
import com.company.orchestrator.infrastructure.edc.connector.EdcConnectorClient;
import com.company.orchestrator.infrastructure.edc.model.TransferProcessResult;
import com.company.orchestrator.infrastructure.edc.model.TransferRequest;
import com.company.orchestrator.infrastructure.events.model.TransferFailedErrorCode;
import com.company.orchestrator.infrastructure.events.publisher.TransferEventPublisher;
import com.company.orchestrator.infrastructure.persistence.entity.TransferRequestEntity;
import com.company.orchestrator.infrastructure.props.EdcProperties;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

import static com.company.orchestrator.domain.utils.Constants.SYSTEM_ACTOR;

/**
 * Controller for handling EDC callbacks and data reception
 * These endpoints are called by the provider EDC connector during the transfer lifecycle
 */
@Log4j2
@Hidden
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiConstants.API_VERSION + "/transfers")
public class EdcCallbackController {

    private final AuditService auditService;
    private final TransferStateService transferStateService;
    private final TransferEventPublisher eventPublisher;
    private final EdcConnectorClient edcClient;
    private final EdcProperties edcProperties;

    /**
     * Endpoint to receive transferred data from provider EDC data plane
     * <p>
     * This endpoint is called by the provider's EDC data plane when data is ready
     * URL: <a href="http://orchestrator:8080/api/v1/transfers/data/receive">...</a>
     *
     * @param transferId Optional transfer ID from query parameter
     * @param headers Request headers (may contain EDC metadata)
     * @param data The actual data payload
     * @return Response indicating data was received
     */
    @PostMapping("/data/receive")
    public ResponseEntity<Map<String, Object>> receiveData(
            @RequestParam(required = false) String transferId,
            @RequestHeader Map<String, String> headers,
            @RequestBody(required = false) byte[] data) {

        log.info("📥 Received data: transferId={}, size={} bytes", transferId, data != null ? data.length : 0);

        try {
            // Extract EDC transfer process ID from headers
            String edcProcessId = headers.getOrDefault("Edc-Transfer-Process-Id",
                    headers.getOrDefault("edc-transfer-process-id", "unknown"));

            // Basic validation
            if (data == null || data.length == 0) {
                log.warn("Empty data payload for transferId={}", transferId);
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "Empty data payload"
                ));
            }

            // Log data preview (first 100 chars)
            String dataPreview = new String(data, 0, Math.min(100, data.length));
            log.info("Data preview: {}", dataPreview);

            // Publish TransferCompleted event (state machine will handle it)
            if (transferId != null && !transferId.isEmpty()) {
                Long transferIdLong = Long.parseLong(transferId);
                TransferRequestEntity transferRequest = transferStateService.getTransferById(transferIdLong);
                eventPublisher.publishTransferCompleted(transferIdLong, transferRequest.getConsumerId(), (long) data.length, edcProcessId);
                log.info("✅ Published TransferCompleted event: transferId={}", transferIdLong);
            }
            // Final step. Data received successfully and can be stored to DB or S3.
            // Transfer flow completed
            return ResponseEntity.ok(Map.of(
                    "status", "received",
                    "transferId", transferId != null ? transferId : "unknown",
                    "bytesReceived", data.length,
                    "message", "Data received successfully",
                    "timestamp", Instant.now().toString()
            ));

        } catch (Exception ex) {
            log.error("❌ Error processing data: transferId={}, error={}", transferId, ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", "error",
                    "message", ex.getMessage()
            ));
        }
    }

    /**
     * Endpoint for EDC contract negotiation and transfer process callbacks
     * <p>
     * This endpoint receives status updates from the provider EDC about:
     * - Contract negotiation status (REQUESTED, CONFIRMED, FINALIZED, etc.)
     * - Transfer process status (STARTED, COMPLETED, FAILED, etc.)
     * <p>
     * URL: <a href="http://orchestrator:8080/api/v1/transfers/callback">...</a>
     *
     * @param payload Callback payload containing status and metadata
     * @return Response acknowledging the callback
     */
    @PostMapping("/callback")
    public ResponseEntity<Map<String, String>> handleEdcCallback(
            @RequestBody Map<String, Object> payload) {

        log.info("Received EDC callback: {}", payload);
        try {
            // Extract common EDC callback fields
            String processId = extractString(payload, "id", "processId", "transferProcessId");
            String state = extractString(payload, "state", "status");
            String type = extractString(payload, "type", "eventType");
            String correlationId = extractString(payload, "correlationId");

            log.info("EDC Callback - processId={}, state={}, type={}, correlationId={}",
                    processId, state, type, correlationId);

            // Map EDC callback states to Transfer states and handle accordingly
            if (state != null && correlationId != null) {
                Long transferId = parseTransferId(correlationId);

                if (transferId != null) {
                    handleStateCallback(transferId, state, processId, payload);
                } else {
                    log.warn("Could not extract transferId from correlationId: {}", correlationId);
                }
            } else {
                log.warn("Missing required callback fields: state={}, correlationId={}", state, correlationId);
            }

            return ResponseEntity.ok(Map.of(
                    "status", "acknowledged",
                    "message", "Callback received and processed"
            ));

        } catch (Exception ex) {
            log.error("Error processing EDC callback: error={}", ex.getMessage(), ex);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "status", "error",
                            "message", "Failed to process callback: " + ex.getMessage()
                    ));
        }
    }

    /**
     * Health check endpoint for EDC to verify callback URL is reachable
     */
    @GetMapping("/callback/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "healthy",
                "service", "EDC Callback Handler"
        ));
    }

    /**
     * Helper method to extract string value from map with multiple possible keys
     */
    private String extractString(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value != null) {
                return value.toString();
            }
        }
        return null;
    }

    /**
     * Handles state callbacks from EDC (Mock or Real)
     * Simplified to handle core states sent by Mock EDC
     */
    private void handleStateCallback(Long transferId, String edcState, String processId,
                                      Map<String, Object> payload) {
        log.info("Handling state callback: transferId={}, edcState={}, processId={}",
                transferId, edcState, processId);

        TransferRequestEntity transfer = transferStateService.getTransferById(transferId);
        try {
            String state = edcState.toUpperCase();

            switch (state) {
                // CONTRACT NEGOTIATION - Contract finalized, initiate transfer
                case "FINALIZED" -> {
                    log.info("Contract finalized for transferId={}", transferId);
                    String agreementId = extractString(payload, "agreementId", "contractAgreementId");
                    String correlationId = extractString(payload, "correlationId");
                    if (agreementId == null) agreementId = processId;
                    eventPublisher.publishContactNegotiated(transferId, transfer.getConsumerId(), agreementId, correlationId);

                    TransferRequest transferRequest = buildTransferRequest(transferId);
                    TransferProcessResult result = edcClient.initiateTransfer(agreementId, transferRequest);

                    if (result != null && result.getTransferProcessId() != null) {
                        transferStateService.updateEdcTransferProcessId(transferId, result.getTransferProcessId());
                        log.info("Transfer initiated: transferId={}, processId={}", transferId, result.getTransferProcessId());
                    } else {
                        eventPublisher.publishTransferFailed(transferId, transfer.getConsumerId(), "Failed to initiate transfer",
                            TransferFailedErrorCode.TRANSFER_INITIATION_FAILED);
                    }
                }

                // TRANSFER IN PROGRESS
                case "PROVISIONING", "STARTED" -> {
                    log.info("Transfer in progress: transferId={}, state={}", transferId, state);
                    if (processId != null) {
                        transferStateService.updateEdcTransferProcessId(transferId, processId);
                    }
                    eventPublisher.publishTransferInProgress(transferId, transfer.getConsumerId(), processId, state);
                }

                // TRANSFER COMPLETED
                case "COMPLETED" ->
                        log.info("Transfer completed: transferId={} Data will be arrived soon", transferId);

                // TRANSFER FAILED
                case "FAILED" -> {
                    log.error("Transfer failed: transferId={}, state={}", transferId, state);
                    String errorMsg = extractString(payload, "errorDetail", "error", "message");
                    eventPublisher.publishTransferFailed(transferId, transfer.getConsumerId(),
                        errorMsg != null ? errorMsg : "Transfer " + state.toLowerCase(),
                        TransferFailedErrorCode.EDC_CALLBACK_FAILED);
                }

                // TRANSFER TERMINATED
                case "TERMINATED" -> {
                    // Immediate action: the termination happens right away.
                    log.error("Transfer canceled: transferId={}, state={}", transferId, state);
                    TransferStatus currentStatus = transfer.getStatus();
                    transferStateService.cancelTransfer(transferId, SYSTEM_ACTOR);
                    auditService.logStateTransition(transferId, transfer.getConsumerId(), currentStatus, TransferStatus.CANCELLED);
                }

                // IGNORE intermediate states
                case "OFFERED", "AGREED" ->
                    log.debug("Intermediate negotiation state: {}", state);

                // UNKNOWN
                default ->
                    log.warn("Unknown EDC state: transferId={}, state={}", transferId, state);
            }

        } catch (Exception ex) {
            log.error("Error handling callback: transferId={}, error={}", transferId, ex.getMessage(), ex);
            eventPublisher.publishTransferFailed(transferId, transfer.getConsumerId(), "Callback processing error: " + ex.getMessage(),
                TransferFailedErrorCode.EDC_CALLBACK_ERROR);
        }
    }

    /**
     * Parses transfer ID from correlation ID or process ID
     */
    private Long parseTransferId(String correlationId) {
        if (correlationId == null || correlationId.isEmpty()) {
            return null;
        }

        try {
            return Long.parseLong(correlationId);
        } catch (NumberFormatException ex) {
            // Try extracting from formatted string (e.g., "transfer-123")
            String[] parts = correlationId.split("-");
            if (parts.length > 0) {
                try {
                    return Long.parseLong(parts[parts.length - 1]);
                } catch (NumberFormatException e) {
                    log.warn("Could not parse transferId from correlationId: {}", correlationId);
                }
            }
        }
        return null;
    }

    /**
     * Builds transfer request for EDC transfer initiation
     * Called from async callback after contract is finalized
     */
    private TransferRequest buildTransferRequest(Long transferId) {
        // Fetch the transfer entity to get actual assetId and providerId
        TransferRequestEntity transferEntity = transferStateService.getTransferById(transferId);

        if (transferEntity == null) {
            log.error("Transfer entity not found for transferId={}", transferId);
            throw new IllegalStateException("Transfer entity not found: " + transferId);
        }

        log.debug("Building transfer request: transferId={}, assetId={}, providerId={}, providerUrl={}",
                transferId, transferEntity.getAssetId(), transferEntity.getProviderId(), edcProperties.getProviderUrl());

        return TransferRequest.builder()
                .assetId(transferEntity.getAssetId())  // Dynamic asset ID from entity
                .providerUrl(edcProperties.getProviderUrl())  // Provider's DSP endpoint
                .destinationType("HttpProxy")
                .destinationUrl(edcProperties.getCallbackBaseUrl() + "/api/v1/transfers/data/receive?transferId=" + transferId)
                .protocol("dataspace-protocol-http")
                .managedTransfer(true)
                .build();
    }
}