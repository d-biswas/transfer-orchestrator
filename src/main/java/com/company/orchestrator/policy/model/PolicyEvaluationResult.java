package com.company.orchestrator.policy.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class PolicyEvaluationResult {
    private final Policy policy;
    private final boolean allowed;
    private final String violationReason;

}
