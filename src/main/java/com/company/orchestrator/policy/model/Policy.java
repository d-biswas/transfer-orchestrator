package com.company.orchestrator.policy.model;

import com.company.orchestrator.domain.model.TransferRequest;

public interface Policy {
    PolicyEvaluationResult evaluate(TransferRequest request);
}
