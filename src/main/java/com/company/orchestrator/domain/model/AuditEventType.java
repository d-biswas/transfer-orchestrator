package com.company.orchestrator.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Objects;

@Getter
@RequiredArgsConstructor
public enum AuditEventType {
    // --- Request Lifecycle ---
    TRANSFER_REQUESTED(1, "Transfer requested"),

    // --- Policy Evaluation ---
    POLICY_EVALUATION_STARTED(10, "Policy evaluation started"),
    POLICY_APPROVED(11, "Policy approved"),
    POLICY_DENIED(12, "Policy denied"),

    // --- Contract Negotiation ---
    CONTRACT_NEGOTIATION_STARTED(20, "Contract negotiation started"),
    CONTRACT_NEGOTIATED(21, "Contract negotiation completed"),
    CONTRACT_NEGOTIATION_FAILED(22, "Contract negotiation failed"),

    // --- Transfer Process ---
    TRANSFER_STARTED(30, "Transfer in progress"),
    TRANSFER_COMPLETED(31, "Transfer completed successfully"),
    TRANSFER_FAILED(32, "Transfer failed"),
    TRANSFER_CANCELLED(33, "Transfer cancelled");

    private final Integer id;
    private final String name;

    public static AuditEventType getById(Integer id) {
        return Arrays.stream(values())
                .filter(type -> Objects.equals(type.id, id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown AuditEventType id: " + id));
    }
}