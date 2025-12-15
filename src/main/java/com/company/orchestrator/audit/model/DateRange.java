package com.company.orchestrator.audit.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Represents a date range for queries
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DateRange {
    private Instant from;
    private Instant to;
}