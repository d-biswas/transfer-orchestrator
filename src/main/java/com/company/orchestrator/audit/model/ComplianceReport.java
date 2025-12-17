package com.company.orchestrator.audit.model;

import com.company.orchestrator.utils.InstantDeserializer;
import com.company.orchestrator.utils.InstantSerializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
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
    @JsonSerialize(using = InstantSerializer.class)
    @JsonDeserialize(using = InstantDeserializer.class)
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