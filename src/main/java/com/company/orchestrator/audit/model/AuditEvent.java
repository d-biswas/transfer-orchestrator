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
    private Long id;
    private Long transferId;
    private String consumerId;
    private AuditEventType eventType;
    private String actor;
    private String message;
    private Map<String, Object> metadata;
    private Instant createdAt;
}