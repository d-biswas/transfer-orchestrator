package com.company.orchestrator.infrastructure.persistence.entity;

import com.company.orchestrator.domain.model.TransferStatus;
import com.company.orchestrator.domain.model.converter.TransferStatusConverter;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serial;
import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "transfer_state_history")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferStateHistoryEntity extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 6160626839751318135L;

    @Column(name = "transfer_id", nullable = false)
    private Long transferId;

    @Column(name = "from_state")
    @Convert(converter = TransferStatusConverter.class)
    private TransferStatus fromState;

    @Column(name = "to_state", nullable = false)
    @Convert(converter = TransferStatusConverter.class)
    private TransferStatus toState;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;
}