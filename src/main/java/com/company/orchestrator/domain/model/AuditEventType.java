package com.company.orchestrator.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Objects;

@Getter
@RequiredArgsConstructor
public enum AuditEventType {
    TRANSFER_REQUESTED(1, "TRANSFER_REQUESTED"),
    POLICY_EVALUATED(2, "POLICY_EVALUATED"),
    TRANSFER_APPROVED(3, "TRANSFER_APPROVED"),
    TRANSFER_DENIED(4, "TRANSFER_DENIED"),
    STATE_CHANGED(5, "STATE_CHANGED"),
    CONTRACT_NEGOTIATED(6, "CONTRACT_NEGOTIATED"),
    TRANSFER_STARTED(7, "TRANSFER_STARTED"),
    TRANSFER_COMPLETED(8, "TRANSFER_COMPLETED"),
    TRANSFER_FAILED(9, "TRANSFER_FAILED"),
    TRANSFER_CANCELLED(10, "TRANSFER_CANCELLED");

    private final Integer id;
    private final String name;

    public static AuditEventType getById(Integer id) {
        return Arrays.stream(values())
                .filter(type -> Objects.equals(type.id, id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown AuditEventType id: " + id));
    }
}