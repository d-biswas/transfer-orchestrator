package com.company.orchestrator.audit.model;

import lombok.*;

import java.time.Instant;
import java.util.Map;

/**
 * Compliance report for audit trail
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceReport {
    private Instant generatedAt;
    private DateRange dateRange;
    private Map<String, ConsumerReport> consumerReports;

    /**
     * Consumer-level summary
     */
    @Getter
    @Setter
    @Builder
    public static class ConsumerReport {
        private long totalTransfers;
        private long successfulTransfers;
        private long failedTransfers;
        private long deniedTransfers;
    }
}