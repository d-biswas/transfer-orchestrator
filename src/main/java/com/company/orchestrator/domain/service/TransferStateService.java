package com.company.orchestrator.domain.service;

import com.company.orchestrator.domain.model.DataType;
import com.company.orchestrator.domain.model.TransferStatus;
import com.company.orchestrator.infrastructure.persistence.entity.TransferRequestEntity;
import com.company.orchestrator.infrastructure.persistence.entity.TransferStateHistoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface TransferStateService {

    Optional<TransferRequestEntity> findTransferById(Long transferId);

    TransferRequestEntity getTransferById(Long transferId);

    TransferRequestEntity initiateTransfer(String consumerId, String providerId, String assetId, DataType dataType);

    void updateState(Long transferId, TransferStatus newStatus, String actor);

    void cancelTransfer(Long transferId, String actor);

    List<TransferStateHistoryEntity> getStateHistory(Long transferId);

    void updateEdcNegotiationId(Long transferId, String edcNegotiationId);

    void updateEdcTransferProcessId(Long transferId, String edcTransferProcessId);

    void updateEdcContractAgreementId(Long transferId, String edcContractAgreementId);

    Page<TransferRequestEntity> findTransfers(Pageable pageable);
}
