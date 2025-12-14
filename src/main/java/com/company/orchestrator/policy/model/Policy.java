package com.company.orchestrator.policy.model;

import com.company.orchestrator.api.request.TransferInitiateDto;

public interface Policy {
    PolicyEvaluationResult evaluate(TransferInitiateDto request);
}
