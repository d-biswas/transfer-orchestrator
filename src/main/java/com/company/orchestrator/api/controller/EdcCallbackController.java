package com.company.orchestrator.api.controller;

import com.company.orchestrator.api.constants.ApiConstants;
import com.company.orchestrator.domain.service.TransferStateService;
import com.company.orchestrator.infrastructure.edc.connector.EdcConnectorClient;
import com.company.orchestrator.infrastructure.edc.model.TransferProcessResult;
import com.company.orchestrator.infrastructure.edc.model.TransferRequest;
import com.company.orchestrator.infrastructure.events.model.TransferFailedErrorCode;
import com.company.orchestrator.infrastructure.events.publisher.TransferEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * Controller for handling EDC callbacks and data reception
 * These endpoints are called by the provider EDC connector during the transfer lifecycle
 */
@Log4j2
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiConstants.API_VERSION + "/transfers")
public class EdcCallbackController {

    private final TransferStateService transferStateService;
    private final TransferEventPublisher eventPublisher;
    private final EdcConnectorClient edcClient;
    private static final String DATA_STORAGE_PATH = "/tmp/orchestrator/transfers"; // TODO: Make configurable

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

        log.info("Received data transfer callback: transferId={}, dataSize={} bytes",
                transferId, data != null ? data.length : 0);

        // Log important headers
        headers.forEach((key, value) -> {
            if (key.toLowerCase().startsWith("edc-") || key.toLowerCase().startsWith("x-")) {
                log.debug("Header: {}={}", key, value);
            }
        });

        try {
            // Extract EDC transfer process ID from headers if available
            String edcTransferProcessId = headers.getOrDefault("Edc-Transfer-Process-Id",
                    headers.getOrDefault("edc-transfer-process-id", "unknown"));

            // Validate data
            if (data == null || data.length == 0) {
                log.warn("Received empty data payload for transferId={}", transferId);
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "status", "error",
                                "transferId", transferId != null ? transferId : "unknown",
                                "message", "Empty data payload"
                        ));
            }

            log.info("Processing received data: transferId={}, dataSize={} bytes, edcProcessId={}",
                    transferId, data.length, edcTransferProcessId);

            // 1. Store data to file system (in production, use S3, Azure Blob, etc.)
            String filePath = storeDataToFileSystem(transferId, data);
            log.info("Data stored to: {}", filePath);

            // 2. Validate data integrity (basic checks)
            boolean isValid = validateDataIntegrity(data);
            if (!isValid) {
                log.error("Data validation failed for transferId={}", transferId);
                handleDataValidationFailure(transferId, "Data integrity check failed");
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                        .body(Map.of(
                                "status", "error",
                                "transferId", transferId != null ? transferId : "unknown",
                                "message", "Data validation failed"
                        ));
            }

            // 3. Publish TransferCompleted event to Kafka (state will be updated by event handler)
            if (transferId != null && !transferId.isEmpty()) {
                try {
                    Long transferIdLong = Long.parseLong(transferId);
                    eventPublisher.publishTransferCompleted(
                            transferIdLong,
                            (long) data.length,
                            edcTransferProcessId
                    );
                    log.info("Published TransferCompleted event: transferId={}", transferIdLong);

                } catch (NumberFormatException ex) {
                    log.error("Invalid transferId format: {}", transferId);
                } catch (Exception ex) {
                    log.error("Error publishing transfer completed event: transferId={}, error={}",
                            transferId, ex.getMessage(), ex);
                }
            }

            // 5. Return success response
            return ResponseEntity.ok(Map.of(
                    "status", "received",
                    "transferId", transferId != null ? transferId : "unknown",
                    "bytesReceived", data.length,
                    "filePath", filePath,
                    "edcTransferProcessId", edcTransferProcessId,
                    "message", "Data received and stored successfully",
                    "timestamp", Instant.now().toString()
            ));

        } catch (Exception ex) {
            log.error("Error processing received data: transferId={}, error={}",
                    transferId, ex.getMessage(), ex);

            // Attempt to mark transfer as FAILED
            if (transferId != null && !transferId.isEmpty()) {
                try {
                    Long transferIdLong = Long.parseLong(transferId);
                    handleDataReceptionFailure(transferIdLong, ex.getMessage());
                } catch (Exception e) {
                    log.error("Failed to update transfer state to FAILED: {}", e.getMessage());
                }
            }

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "status", "error",
                            "transferId", transferId != null ? transferId : "unknown",
                            "message", "Failed to process data: " + ex.getMessage(),
                            "timestamp", Instant.now().toString()
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
     * Stores received data to file system
     * In production, replace with S3, Azure Blob Storage, or database storage
     */
    private String storeDataToFileSystem(String transferId, byte[] data) throws IOException {
        // Create storage directory if it doesn't exist
        Path storageDir = Paths.get(DATA_STORAGE_PATH);
        if (!Files.exists(storageDir)) {
            Files.createDirectories(storageDir);
            log.info("Created storage directory: {}", storageDir);
        }

        // Generate filename with timestamp
        String filename = String.format("transfer_%s_%d.dat",
                transferId != null ? transferId : "unknown",
                Instant.now().toEpochMilli());

        Path filePath = storageDir.resolve(filename);

        // Write data to file
        Files.write(filePath, data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        log.debug("Stored {} bytes to file: {}", data.length, filePath);
        return filePath.toString();
    }

    /**
     * Validates data integrity
     * Basic checks: only size limits
     */
    private boolean validateDataIntegrity(byte[] data) {
        if (data == null || data.length == 0) {
            log.warn("Data validation failed: empty data");
            return false;
        }

        // Basic validation - check size limits (e.g., max 100MB)
        long maxSizeBytes = 100 * 1024 * 1024; // 100MB
        if (data.length > maxSizeBytes) {
            log.error("Data validation failed: size {} exceeds limit {}", data.length, maxSizeBytes);
            return false;
        }
        log.debug("Data validation passed: size={} bytes", data.length);
        return true;
    }

    /**
     * Handles state callbacks from EDC
     * Maps EDC states to Kafka events - does NOT update state directly
     * State updates are handled by TransferEventHandler
     */
    private void handleStateCallback(Long transferId, String edcState, String processId,
                                      Map<String, Object> payload) {
        log.info("Handling state callback: transferId={}, edcState={}, processId={}",
                transferId, edcState, processId);

        try {
            String edcStateUpper = edcState.toUpperCase();

            switch (edcStateUpper) {
                // Contract Negotiation States
                case "FINALIZED":
                case "AGREED":
                    // Contract agreement finalized - publish event to update to NEGOTIATED
                    log.info("Contract finalized for transferId={}, agreementId={}", transferId, processId);

                    // Initiate the actual data transfer now that contract is agreed
                    try {
                        String agreementId = extractString(payload, "agreementId", "contractAgreementId");
                        if (agreementId == null) {
                            agreementId = processId; // Fallback to processId
                        }

                        // Store contract agreement ID
                        transferStateService.updateEdcContractAgreementId(transferId, agreementId);
                        log.debug("Stored EDC contract agreement ID: transferId={}, agreementId={}", transferId, agreementId);

                        log.info("Initiating data transfer for transferId={}, agreementId={}", transferId, agreementId);// Build transfer request
                        TransferRequest transferRequest = buildTransferRequest(transferId);
                        TransferProcessResult result = edcClient.initiateTransfer(agreementId, transferRequest);

                        if (result != null && result.getTransferProcessId() != null) {
                            log.info("Data transfer initiated successfully: transferId={}, transferProcessId={}",
                                    transferId, result.getTransferProcessId());

                            transferStateService.updateEdcTransferProcessId(transferId, result.getTransferProcessId());
                            log.debug("Stored EDC transfer process ID: transferId={}, processId={}",
                                    transferId, result.getTransferProcessId());

                            // State will be updated to TRANSFER_IN_PROGRESS via subsequent EDC callbacks
                        } else {
                            log.error("Failed to initiate data transfer for transferId={}", transferId);
                            eventPublisher.publishTransferFailed(
                                    transferId,
                                    "Failed to initiate data transfer after contract agreement",
                                    TransferFailedErrorCode.TRANSFER_INITIATION_FAILED
                            );
                        }
                    } catch (Exception ex) {
                        log.error("Error initiating data transfer for transferId={}: {}", transferId, ex.getMessage(), ex);
                        eventPublisher.publishTransferFailed(
                                transferId,
                                "Error initiating data transfer: " + ex.getMessage(),
                                TransferFailedErrorCode.TRANSFER_INITIATION_ERROR
                        );
                    }
                    break;

                // Transfer Process States
                case "STARTED":
                case "PROVISIONING":
                case "PROVISIONED":
                case "IN_PROGRESS":
                case "STREAMING":
                    // Transfer process started/in progress - publish event
                    log.info("Transfer process in progress for transferId={}, edcState={}", transferId, edcStateUpper);

                    // Store transfer process ID if not already stored
                    if (processId != null && !processId.isEmpty()) {
                        transferStateService.updateEdcTransferProcessId(transferId, processId);
                        log.debug("Ensured EDC transfer process ID is stored: transferId={}, processId={}",
                                transferId, processId);
                    }
                    eventPublisher.publishTransferInProgress(transferId, processId, edcStateUpper);
                    break;

                case "COMPLETED":
                    // Transfer completed successfully - publish event
                    log.info("Transfer completed via callback for transferId={}", transferId);
                    eventPublisher.publishTransferCompleted(transferId, 0L, processId);
                    break;

                case "FAILED":
                case "TERMINATED":
                case "ERROR":
                    log.error("Transfer failed via callback for transferId={}, edcState={}", transferId, edcStateUpper);
                    String errorMessage = extractString(payload, "errorDetail", "error", "message");

                    TransferFailedErrorCode errorCode = switch (edcStateUpper) {
                        case "FAILED" -> TransferFailedErrorCode.EDC_CALLBACK_FAILED;
                        case "TERMINATED" -> TransferFailedErrorCode.EDC_CALLBACK_TERMINATED;
                        default -> TransferFailedErrorCode.EDC_CALLBACK_ERROR;
                    };

                    eventPublisher.publishTransferFailed(
                            transferId,
                            Optional.ofNullable(errorMessage).orElse("Transfer terminated by EDC"),
                            errorCode
                    );
                    break;

                case "DEPROVISIONING":
                case "DEPROVISIONED":
                    log.info("Transfer cleanup phase for transferId={}, edcState={}", transferId, edcStateUpper);
                    break;

                default:
                    log.debug("Unhandled EDC state callback: state={}, transferId={}", edcState, transferId);
            }

        } catch (Exception ex) {
            log.error("Error handling state callback for transferId={}: {}",
                    transferId, ex.getMessage(), ex);
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
     * Handles data validation failure - publishes event only (no direct state update)
     */
    private void handleDataValidationFailure(String transferId, String reason) {
        log.error("Data validation failed: transferId={}, reason={}", transferId, reason);

        if (transferId != null && !transferId.isEmpty()) {
            try {
                Long transferIdLong = Long.parseLong(transferId);
                eventPublisher.publishTransferFailed(
                        transferIdLong,
                        "Data validation failed: " + reason,
                        TransferFailedErrorCode.DATA_VALIDATION_ERROR
                );
            } catch (Exception ex) {
                log.error("Failed to handle validation failure: {}", ex.getMessage());
            }
        }
    }

    /**
     * Handles data reception failure - publishes event only (no direct state update)
     */
    private void handleDataReceptionFailure(Long transferId, String errorMessage) {
        log.error("Data reception failed: transferId={}, error={}", transferId, errorMessage);

        try {
            eventPublisher.publishTransferFailed(
                    transferId,
                    "Data reception failed: " + errorMessage,
                    TransferFailedErrorCode.DATA_RECEPTION_ERROR
            );
        } catch (Exception ex) {
            log.error("Failed to handle reception failure: {}", ex.getMessage());
        }
    }

    /**
     * Builds transfer request for EDC transfer initiation
     * Called from async callback after contract is finalized
     */
    private TransferRequest buildTransferRequest(Long transferId) {
        return TransferRequest.builder()
                .assetId("asset-" + transferId) // TODO: Get actual asset ID from transfer entity
                .providerUrl("http://provider-edc-controlplane:8282/api/v1/dsp")
                .destinationType("HttpProxy")
                .destinationUrl("http://orchestrator:8080/api/v1/transfers/data/receive?transferId=" + transferId)
                .protocol("dataspace-protocol-http")
                .managedTransfer(true)
                .build();
    }
}