package com.company.orchestrator.audit.model;

import com.company.orchestrator.infrastructure.utils.InstantDeserializer;
import com.company.orchestrator.infrastructure.utils.InstantSerializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
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
    @JsonSerialize(using = InstantSerializer.class)
    @JsonDeserialize(using = InstantDeserializer.class)
    private Instant from;

    @JsonSerialize(using = InstantSerializer.class)
    @JsonDeserialize(using = InstantDeserializer.class)
    private Instant to;
}