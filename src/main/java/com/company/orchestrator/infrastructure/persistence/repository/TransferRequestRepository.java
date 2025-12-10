package com.company.orchestrator.infrastructure.persistence.repository;

import com.company.orchestrator.domain.model.TransferStatus;
import com.company.orchestrator.infrastructure.persistence.entity.TransferRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransferRequestRepository extends JpaRepository<TransferRequestEntity, Long> {

    List<TransferRequestEntity> findByConsumerId(String consumerId);

    List<TransferRequestEntity> findByProviderId(String providerId);

    List<TransferRequestEntity> findByStatus(TransferStatus status);

    List<TransferRequestEntity> findByStatusIn(List<TransferStatus> statuses);

    Optional<TransferRequestEntity> findByIdAndConsumerId(Long id, String consumerId);
}