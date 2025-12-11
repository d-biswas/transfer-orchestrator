package com.company.orchestrator.policy.model;

import com.company.orchestrator.domain.model.TransferRequest;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CertificationPolicy implements Policy {

    private final String requiredCertification;

    @Override
    public PolicyEvaluationResult evaluate(TransferRequest request) {
        boolean allowed = request.getConsumerCertification().equalsIgnoreCase(requiredCertification);
        return new PolicyEvaluationResult(this, allowed,
            allowed ? null : "Consumer lacks required certification");
    }
}
