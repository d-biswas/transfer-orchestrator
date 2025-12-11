package com.company.orchestrator.policy.model;

import com.company.orchestrator.api.dto.TransferRequestDto;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class OrPolicy implements Policy {

    private final List<Policy> policies;

    @Override
    public PolicyEvaluationResult evaluate(TransferRequestDto request) {
        StringBuilder violations = new StringBuilder();
        for (Policy p : policies) {
            PolicyEvaluationResult result = p.evaluate(request);
            if (result.allowed()) {
                return new PolicyEvaluationResult(this, true, null);
            } else {
                violations.append(result.violationReason()).append("; ");
            }
        }
        return new PolicyEvaluationResult(this, false, violations.toString());
    }
}
