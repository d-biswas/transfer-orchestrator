package com.company.orchestrator.infrastructure.edc.connector.impl;

import com.company.orchestrator.infrastructure.edc.connector.EdcConnectorClient;
import com.company.orchestrator.infrastructure.edc.model.*;
import com.company.orchestrator.infrastructure.props.EdcProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * WebClient-based implementation of EDC Connector Client
 * Communicates with Eclipse Dataspace Connector via Management API
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class EdcConnectorClientImpl implements EdcConnectorClient {

    private final WebClient edcWebClient;
    private final EdcProperties edcProperties;

    private static final String EDC_CONTEXT = "https://w3id.org/edc/v0.0.1/ns/";
    private static final String CONTRACT_NEGOTIATIONS_PATH = "/v2/contractnegotiations";
    private static final String TRANSFER_PROCESSES_PATH = "/v2/transferprocesses";

    @Override
    @SuppressWarnings("unchecked")
    public ContractNegotiationResult negotiateContract(ContractOffer offer) {
        log.info("Initiating async contract negotiation for asset: {} with provider: {}",
                offer.getAssetId(), offer.getProviderId());

        try {
            // Build contract negotiation request
            Map<String, Object> request = buildContractNegotiationRequest(offer);

            // Send negotiation request ASYNCHRONOUSLY - do not block
            // EDC will send status updates to the callback URL provided in the request
            Map<String, Object> response = edcWebClient.post()
                    .uri(CONTRACT_NEGOTIATIONS_PATH)
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse ->
                            clientResponse.bodyToMono(String.class)
                                    .flatMap(errorBody -> {
                                        log.error("Contract negotiation failed: {}", errorBody);
                                        return Mono.error(new RuntimeException("EDC contract negotiation failed: " + errorBody));
                                    })
                    )
                    .bodyToMono(Map.class)
                    .retryWhen(Retry.backoff(edcProperties.getMaxRetries(),
                            Duration.ofMillis(edcProperties.getRetryBackoffMillis())))
                    .block(Duration.ofSeconds(10)); // Only wait for initial response, not completion

            if (response == null) {
                throw new IllegalStateException("EDC returned null response");
            }

            String negotiationId = (String) response.get("@id");
            String state = (String) response.get("state");

            if (negotiationId == null) {
                throw new IllegalStateException("EDC response missing negotiation ID");
            }

            log.info("Contract negotiation initiated successfully (async). NegotiationId: {}, Initial State: {}",
                    negotiationId, state);
            log.info("EDC will send status updates to callback URL: {}", offer.getConsumerCallbackUrl());

            // Return immediately with negotiation ID - do NOT wait for completion
            // Further state updates will come via EDC callbacks to EdcCallbackController
            return ContractNegotiationResult.success(negotiationId, null, state);

        } catch (Exception e) {
            log.error("Failed to initiate contract negotiation for asset: {}", offer.getAssetId(), e);
            return ContractNegotiationResult.failure(null, "FAILED", e.getMessage());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public TransferProcessResult initiateTransfer(String agreementId, TransferRequest request) {
        log.info("Initiating transfer for agreement: {}, asset: {}", agreementId, request.getAssetId());

        try {
            // Build transfer request
            Map<String, Object> transferRequest = buildTransferRequest(agreementId, request);

            // Send transfer initiation request
            Map<String, Object> response = edcWebClient.post()
                    .uri(TRANSFER_PROCESSES_PATH)
                    .bodyValue(transferRequest)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse ->
                            clientResponse.bodyToMono(String.class)
                                    .flatMap(errorBody -> {
                                        log.error("Transfer initiation failed: {}", errorBody);
                                        return Mono.error(new RuntimeException("EDC transfer initiation failed: " + errorBody));
                                    })
                    )
                    .bodyToMono(Map.class)
                    .retryWhen(Retry.backoff(edcProperties.getMaxRetries(),
                            Duration.ofMillis(edcProperties.getRetryBackoffMillis())))
                    .block(Duration.ofSeconds(30));

            if (response == null) {
                throw new IllegalStateException("EDC returned null response");
            }

            String transferProcessId = (String) response.get("@id");
            String state = (String) response.get("state");

            if (transferProcessId == null) {
                throw new IllegalStateException("EDC response missing transfer process ID");
            }

            log.info("Transfer initiated successfully. TransferProcessId: {}, State: {}",
                    transferProcessId, state);

            return TransferProcessResult.success(transferProcessId, state);

        } catch (Exception e) {
            log.error("Failed to initiate transfer for agreement: {}", agreementId, e);
            return TransferProcessResult.failure(null, "FAILED", e.getMessage());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public TransferProcessState getTransferState(String transferProcessId) {
        log.debug("Fetching transfer state for: {}", transferProcessId);

        try {
            Map<String, Object> response = edcWebClient.get()
                    .uri(TRANSFER_PROCESSES_PATH + "/{id}", transferProcessId)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse ->
                            clientResponse.bodyToMono(String.class)
                                    .flatMap(errorBody -> {
                                        log.error("Failed to fetch transfer state: {}", errorBody);
                                        return Mono.error(new RuntimeException("Failed to get transfer state: " + errorBody));
                                    })
                    )
                    .bodyToMono(Map.class)
                    .block(Duration.ofSeconds(10));

            if (response == null) {
                throw new IllegalStateException("EDC returned null response");
            }

            return TransferProcessState.builder()
                    .transferProcessId((String) response.get("@id"))
                    .state((String) response.get("state"))
                    .stateTimestamp((Long) response.get("stateTimestamp"))
                    .errorDetail((String) response.get("errorDetail"))
                    .build();

        } catch (Exception e) {
            log.error("Failed to get transfer state for: {}", transferProcessId, e);
            return TransferProcessState.builder()
                    .transferProcessId(transferProcessId)
                    .state("UNKNOWN")
                    .errorDetail(e.getMessage())
                    .build();
        }
    }

    @Override
    public void terminateTransfer(String transferProcessId) {
        log.info("Terminating transfer process: {}", transferProcessId);

        try {
            Map<String, Object> terminateRequest = new HashMap<>();
            terminateRequest.put("@context", EDC_CONTEXT);
            terminateRequest.put("@type", "TerminateTransfer");
            terminateRequest.put("reason", "Terminated by orchestrator");

            edcWebClient.post()
                    .uri(TRANSFER_PROCESSES_PATH + "/{id}/terminate", transferProcessId)
                    .bodyValue(terminateRequest)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse ->
                            clientResponse.bodyToMono(String.class)
                                    .flatMap(errorBody -> {
                                        log.error("Failed to terminate transfer: {}", errorBody);
                                        return Mono.error(new RuntimeException("Failed to terminate transfer: " + errorBody));
                                    })
                    )
                    .bodyToMono(Void.class)
                    .block(Duration.ofSeconds(10));

            log.info("Transfer process terminated successfully: {}", transferProcessId);

        } catch (Exception e) {
            log.error("Failed to terminate transfer: {}", transferProcessId, e);
            throw new RuntimeException("Failed to terminate transfer", e);
        }
    }

    /**
     * Builds EDC Management API contract negotiation request
     */
    private Map<String, Object> buildContractNegotiationRequest(ContractOffer offer) {
        Map<String, Object> policy = new HashMap<>();
        policy.put("@type", "Offer");
        policy.put("@id", offer.getOfferId());
        policy.put("assigner", offer.getProviderId());
        policy.put("target", offer.getAssetId());

        Map<String, Object> request = new HashMap<>();
        request.put("@context", EDC_CONTEXT);
        request.put("@type", "ContractRequest");
        request.put("counterPartyAddress", offer.getProviderUrl());
        request.put("protocol", "dataspace-protocol-http");
        request.put("counterPartyId", offer.getProviderId());
        request.put("policy", policy);

        if (offer.getConsumerCallbackUrl() != null) {
            Map<String, String> callback = new HashMap<>();
            callback.put("uri", offer.getConsumerCallbackUrl());
            request.put("callbackAddresses", java.util.List.of(callback));
        }

        return request;
    }

    /**
     * Builds EDC Management API transfer request
     */
    private Map<String, Object> buildTransferRequest(String agreementId, TransferRequest request) {
        Map<String, Object> dataDestination = new HashMap<>();
        dataDestination.put("type", request.getDestinationType() != null ? request.getDestinationType() : "HttpProxy");
        if (request.getDestinationUrl() != null) {
            dataDestination.put("baseUrl", request.getDestinationUrl());
        }

        Map<String, Object> transferReq = new HashMap<>();
        transferReq.put("@context", EDC_CONTEXT);
        transferReq.put("@type", "TransferRequest");
        transferReq.put("contractId", agreementId);
        transferReq.put("assetId", request.getAssetId());
        transferReq.put("counterPartyAddress", request.getProviderUrl());
        transferReq.put("protocol", request.getProtocol() != null ? request.getProtocol() : "dataspace-protocol-http");
        transferReq.put("dataDestination", dataDestination);
        transferReq.put("managedResources", request.isManagedTransfer());

        return transferReq;
    }
}