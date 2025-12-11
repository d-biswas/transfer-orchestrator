package com.company.orchestrator.policy.model;

import com.company.orchestrator.domain.model.TransferRequest;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class NotPolicy implements Policy {

    private final Policy policy;

    @Override
    public PolicyEvaluationResult evaluate(TransferRequest request) {
        PolicyEvaluationResult result = policy.evaluate(request);
        boolean allowed = !result.allowed();
        return new PolicyEvaluationResult(this, allowed,
            allowed ? null : "Negated policy violated: " + result.violationReason());
    }
}
