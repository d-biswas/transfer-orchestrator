package com.company.orchestrator.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Objects;

@Getter
@RequiredArgsConstructor
public enum AuditEventType {
    // --- Request Lifecycle ---
    TRANSFER_REQUESTED(1, "TRANSFER_REQUESTED"),

    // --- Policy Evaluation ---
    POLICY_EVALUATION_STARTED(10, "POLICY_EVALUATION_STARTED"),
    POLICY_EVALUATION_PASSED(11, "POLICY_EVALUATION_PASSED"),
    POLICY_EVALUATION_FAILED(12, "POLICY_EVALUATION_FAILED"),

    // --- Contract Negotiation ---
    CONTRACT_NEGOTIATION_STARTED(20, "CONTRACT_NEGOTIATION_STARTED"),
    CONTRACT_NEGOTIATION_COMPLETED(21, "CONTRACT_NEGOTIATION_COMPLETED"),
    CONTRACT_NEGOTIATION_FAILED(22, "CONTRACT_NEGOTIATION_FAILED"),

    STATE_CHANGED(30, "STATE_CHANGED"),

    // --- Transfer Process ---
    TRANSFER_PROCESS_STARTED(40, "TRANSFER_PROCESS_STARTED"),
    TRANSFER_PROCESS_COMPLETED(41, "TRANSFER_PROCESS_COMPLETED"),
    TRANSFER_PROCESS_FAILED(42, "TRANSFER_PROCESS_FAILED"),
    TRANSFER_PROCESS_CANCELLED(43, "TRANSFER_PROCESS_CANCELLED");

    private final Integer id;
    private final String name;

    public static AuditEventType getById(Integer id) {
        return Arrays.stream(values())
                .filter(type -> Objects.equals(type.id, id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown AuditEventType id: " + id));
    }
}