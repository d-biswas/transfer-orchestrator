package com.company.orchestrator.infrastructure.persistence.entity;

import com.company.orchestrator.domain.model.AuditEventType;
import com.company.orchestrator.domain.model.converter.AuditEventTypeConverter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serial;
import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogEntity extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 6638246574655365583L;

    @Column(name = "transfer_id")
    private Long transferId;

    @Column(name = "event_type", nullable = false)
    @Convert(converter = AuditEventTypeConverter.class)
    private AuditEventType eventType;

    @Column(name = "actor", nullable = false, length = 100)
    private String actor;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}