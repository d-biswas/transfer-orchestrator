package com.company.orchestrator.policy.model;

import com.company.orchestrator.domain.model.TransferRequest;
import lombok.AllArgsConstructor;

import java.util.List;

@AllArgsConstructor
public class OrPolicy implements Policy {

    private final List<Policy> policies;

    @Override
    public PolicyEvaluationResult evaluate(TransferRequest request) {
        StringBuilder violations = new StringBuilder();
        for (Policy p : policies) {
            PolicyEvaluationResult result = p.evaluate(request);
            if (result.isAllowed()) {
                return new PolicyEvaluationResult(this, true, null);
            } else {
                violations.append(result.getViolationReason()).append("; ");
            }
        }
        return new PolicyEvaluationResult(this, false, violations.toString());
    }
}
