package com.company.orchestrator.infrastructure.persistence.repository;

import com.company.orchestrator.infrastructure.persistence.entity.TransferRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransferRequestRepository extends JpaRepository<TransferRequestEntity, Long> {

}