package com.company.orchestrator.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Objects;

@Getter
@RequiredArgsConstructor
public enum TransferStatus {
    REQUESTED(1, "REQUESTED"),
    POLICY_EVALUATION(2, "POLICY_EVALUATION"),
    APPROVED(3, "APPROVED"),
    DENIED(4, "DENIED"),
    CONTRACT_NEGOTIATION(5, "CONTRACT_NEGOTIATION"),
    NEGOTIATED(6, "NEGOTIATED"),
    TRANSFER_IN_PROGRESS(7, "TRANSFER_IN_PROGRESS"),
    COMPLETED(8, "COMPLETED"),
    FAILED(9, "FAILED"),
    CANCELLED(10, "CANCELLED");

    private final Integer id;
    private final String name;

    public static TransferStatus getById(Integer id) {
        return Arrays.stream(values())
                .filter(status -> Objects.equals(status.id, id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown TransferStatus id: " + id));
    }
}