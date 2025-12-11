package com.company.orchestrator.policy.model;

public record PolicyEvaluationResult(
    Policy policy,
    boolean allowed,
    String violationReason
) {}