package com.company.orchestrator.policy.model;

import com.company.orchestrator.api.request.TransferInitiateDto;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UsagePolicy implements Policy {

    private final String allowedUsage;

    @Override
    public PolicyEvaluationResult evaluate(TransferInitiateDto request) {
        boolean allowed = request.getUsagePurpose().equalsIgnoreCase(allowedUsage);
        return new PolicyEvaluationResult(this, allowed,
            allowed ? null : "Data usage not allowed for this purpose");
    }
}
