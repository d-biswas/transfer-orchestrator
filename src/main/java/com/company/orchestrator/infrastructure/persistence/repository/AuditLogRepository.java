package com.company.orchestrator.infrastructure.persistence.repository;

import com.company.orchestrator.domain.model.AuditEventType;
import com.company.orchestrator.infrastructure.persistence.entity.AuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long> {

    List<AuditLogEntity> findByTransferIdOrderByCreatedAtDesc(Long transferId);

    List<AuditLogEntity> findByEventTypeOrderByCreatedAtDesc(AuditEventType eventType);

    List<AuditLogEntity> findByActorOrderByCreatedAtDesc(String actor);
}