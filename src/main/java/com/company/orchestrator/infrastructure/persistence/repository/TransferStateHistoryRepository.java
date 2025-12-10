package com.company.orchestrator.infrastructure.persistence.repository;

import com.company.orchestrator.infrastructure.persistence.entity.TransferStateHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransferStateHistoryRepository extends JpaRepository<TransferStateHistoryEntity, Long> {

    List<TransferStateHistoryEntity> findByTransferIdOrderByChangedAtDesc(Long transferId);

    List<TransferStateHistoryEntity> findByTransferIdOrderByChangedAtAsc(Long transferId);
}