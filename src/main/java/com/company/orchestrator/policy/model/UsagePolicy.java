package com.company.orchestrator.policy.model;

import com.company.orchestrator.domain.model.TransferRequest;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class UsagePolicy implements Policy {

    private final String allowedUsage;

    @Override
    public PolicyEvaluationResult evaluate(TransferRequest request) {
        boolean allowed = request.getUsagePurpose().equalsIgnoreCase(allowedUsage);
        return new PolicyEvaluationResult(this, allowed,
            allowed ? null : "Data usage not allowed for this purpose");
    }
}
