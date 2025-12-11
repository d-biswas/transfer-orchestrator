package com.company.orchestrator.audit.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Compliance report for audit trail
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceReport {

    /** Report generation timestamp */
    private Instant generatedAt;

    /** Date range covered */
    private DateRange dateRange;

    /** Total number of transfers */
    private long totalTransfers;

    /** Number of successful transfers */
    private long successfulTransfers;

    /** Number of failed transfers */
    private long failedTransfers;

    /** Number of denied transfers (policy violations) */
    private long deniedTransfers;

    /** Policy violation summary */
    private Map<String, Long> policyViolations;

    /** All audit events in the period */
    private List<AuditEvent> events;
}