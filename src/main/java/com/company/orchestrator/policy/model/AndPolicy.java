package com.company.orchestrator.policy.model;

import com.company.orchestrator.api.dto.TransferRequestDto;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.ArrayList;

@RequiredArgsConstructor
public class AndPolicy implements Policy {

    private final List<Policy> policies;

    @Override
    public PolicyEvaluationResult evaluate(TransferRequestDto request) {
        List<String> violations = new ArrayList<>();
        for (Policy p : policies) {
            PolicyEvaluationResult result = p.evaluate(request);
            if (!result.allowed()) {
                violations.add(result.violationReason());
            }
        }
        boolean allowed = violations.isEmpty();
        return new PolicyEvaluationResult(this, allowed,
            allowed ? null : String.join("; ", violations));
    }
}
