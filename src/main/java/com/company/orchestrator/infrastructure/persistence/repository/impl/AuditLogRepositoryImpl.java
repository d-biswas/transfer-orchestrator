package com.company.orchestrator.infrastructure.persistence.repository.impl;

import com.company.orchestrator.audit.model.ComplianceReport;
import com.company.orchestrator.audit.model.DateRange;
import com.company.orchestrator.domain.model.AuditEventType;
import com.company.orchestrator.infrastructure.persistence.entity.QAuditLogEntity;
import com.company.orchestrator.infrastructure.persistence.repository.AuditLogRepositoryCustom;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.*;

@Repository
@RequiredArgsConstructor
public class AuditLogRepositoryImpl implements AuditLogRepositoryCustom {

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public ComplianceReport generateComplianceReport(DateRange dateRange) {
        QAuditLogEntity audit = QAuditLogEntity.auditLogEntity;

        NumberExpression<Long> totalTransfers = audit.id.count();
        NumberExpression<Long> successfulTransfers = new CaseBuilder()
                .when(audit.eventType.eq(AuditEventType.TRANSFER_PROCESS_COMPLETED))
                .then(1L)
                .otherwise(0L)
                .sum();
        NumberExpression<Long> failedTransfers = new CaseBuilder()
                .when(audit.eventType.eq(AuditEventType.TRANSFER_PROCESS_FAILED))
                .then(1L)
                .otherwise(0L)
                .sum();
        NumberExpression<Long> deniedTransfers = new CaseBuilder()
                .when(audit.eventType.eq(AuditEventType.POLICY_EVALUATION_FAILED))
                .then(1L)
                .otherwise(0L)
                .sum();

        List<Tuple> results = jpaQueryFactory
                .select(audit.consumerId,
                        totalTransfers,
                        successfulTransfers,
                        failedTransfers,
                        deniedTransfers)
                .from(audit)
                .where(audit.createdAt.between(dateRange.getFrom(), dateRange.getTo()))
                .groupBy(audit.consumerId)
                .fetch();

        Map<String, ComplianceReport.ConsumerReport> consumerReports = new HashMap<>();
        for (Tuple tuple : results) {
            String consumerId = tuple.get(audit.consumerId);
            if (consumerId == null) consumerId = "UNKNOWN_CONSUMER";

            ComplianceReport.ConsumerReport report = ComplianceReport.ConsumerReport.builder()
                    .totalTransfers(Optional.ofNullable(tuple.get(totalTransfers)).orElse(0L))
                    .successfulTransfers(Optional.ofNullable(tuple.get(successfulTransfers)).orElse(0L))
                    .failedTransfers(Optional.ofNullable(tuple.get(failedTransfers)).orElse(0L))
                    .deniedTransfers(Optional.ofNullable(tuple.get(deniedTransfers)).orElse(0L))
                    .build();

            consumerReports.put(consumerId, report);
        }
        return ComplianceReport.builder()
                .generatedAt(Instant.now())
                .dateRange(dateRange)
                .consumerReports(consumerReports)
                .build();
    }
}
