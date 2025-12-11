package com.company.orchestrator.infrastructure.events.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * Base class for all transfer-related Kafka events
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseTransferEvent implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /** Transfer ID */
    private Long transferId;

    /** Event timestamp */
    private Instant timestamp;

    /** Actor who triggered the event */
    private String actor;

    /** Event type for routing */
    public abstract String getEventType();
}