package com.company.orchestrator.infrastructure.persistence.entity;

import java.time.Instant;

public interface Auditable {
    Instant getCreatedAt();

    void setCreatedAt(Instant dateTime);

    Instant getModifiedAt();

    void setModifiedAt(Instant dateTime);
}
