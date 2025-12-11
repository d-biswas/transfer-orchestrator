package com.company.orchestrator.audit.model;

import com.company.orchestrator.domain.model.AuditEventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Represents a single audit event
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEvent {

    /** Unique audit log ID */
    private Long id;

    /** Associated transfer ID */
    private Long transferId;

    /** Type of audit event */
    private AuditEventType eventType;

    /** Actor who triggered the event */
    private String actor;

    /** Event message */
    private String message;

    /** Additional metadata */
    private Map<String, Object> metadata;

    /** Timestamp when event occurred */
    private Instant createdAt;
}