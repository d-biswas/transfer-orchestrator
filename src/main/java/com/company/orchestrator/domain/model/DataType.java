package com.company.orchestrator.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Objects;

@Getter
@RequiredArgsConstructor
public enum DataType {
    TELEMETRY(1, "TELEMETRY"),
    QUALITY(2, "QUALITY"),
    SUPPLY_CHAIN(3, "SUPPLY_CHAIN"),
    INVOICE(4, "INVOICE"),
    CERTIFICATE(5, "CERTIFICATE"),
    TRACE(6, "TRACE"),
    OTHER(99, "OTHER");

    private final Integer id;
    private final String name;

    public static DataType getById(Integer id) {
        return Arrays.stream(values())
                .filter(type -> Objects.equals(type.id, id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown DataType id: " + id));
    }
}