package com.company.orchestrator.infrastructure.edc.connector;

import com.company.orchestrator.infrastructure.edc.model.ContractNegotiationResult;
import com.company.orchestrator.infrastructure.edc.model.ContractOffer;
import com.company.orchestrator.infrastructure.edc.model.TransferProcessResult;
import com.company.orchestrator.infrastructure.edc.model.TransferProcessState;
import com.company.orchestrator.infrastructure.edc.model.TransferRequest;

/**
 * Client interface for communicating with Eclipse Dataspace Connector (EDC)
 * Handles contract negotiation, transfer initiation, and monitoring
 */
public interface EdcConnectorClient {

    /**
     * Negotiates contract with consumer connector
     *
     * @param offer Contract offer containing provider details and asset information
     * @return Result of the contract negotiation including negotiation ID and agreement ID
     */
    ContractNegotiationResult negotiateContract(ContractOffer offer);

    /**
     * Initiates data transfer after successful negotiation
     *
     * @param agreementId The contract agreement ID from successful negotiation
     * @param request Transfer request with destination and protocol details
     * @return Result of transfer initiation including transfer process ID
     */
    TransferProcessResult initiateTransfer(String agreementId, TransferRequest request);

    /**
     * Monitors transfer process status
     *
     * @param transferProcessId The unique identifier of the transfer process
     * @return Current state of the transfer process
     */
    TransferProcessState getTransferState(String transferProcessId);

    /**
     * Cancels ongoing transfer
     *
     * @param transferProcessId The unique identifier of the transfer process to terminate
     */
    void terminateTransfer(String transferProcessId);
}