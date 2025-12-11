package com.company.orchestrator.domain.model.events;

import com.company.orchestrator.domain.model.TransferStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

@Getter
@Setter
@RequiredArgsConstructor
public class TransferStateEvent implements Serializable {
    @Serial
    private static final long serialVersionUID = 3069930532383327152L;
    private final Long transferId;
    private final TransferStatus status;
    private final Instant timestamp;
    private final String actor; // who triggered the event
}
