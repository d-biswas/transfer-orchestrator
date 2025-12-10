package com.company.orchestrator.infrastructure.persistence.entity;

import com.company.orchestrator.domain.model.DataType;
import com.company.orchestrator.domain.model.TransferStatus;
import com.company.orchestrator.domain.model.converter.DataTypeConverter;
import com.company.orchestrator.domain.model.converter.TransferStatusConverter;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serial;
import java.time.Instant;

@Entity
@Table(name = "transfer_requests")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferRequestEntity extends BaseEntity implements Auditable, Versionable {
    @Serial
    private static final long serialVersionUID = -5913515077419185625L;

    @Column(name = "consumer_id", nullable = false, length = 100)
    private String consumerId;

    @Column(name = "provider_id", nullable = false, length = 100)
    private String providerId;

    @Column(name = "asset_id", nullable = false, length = 100)
    private String assetId;

    @Column(name = "data_type", nullable = false)
    @Convert(converter = DataTypeConverter.class)
    private DataType dataType;

    @Column(name = "status", nullable = false)
    @Convert(converter = TransferStatusConverter.class)
    private TransferStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant modifiedAt;

    @Version
    @Column(name = "version")
    private Long version;
}