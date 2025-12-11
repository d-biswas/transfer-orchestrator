package com.company.orchestrator.policy.model;

import com.company.orchestrator.api.dto.TransferRequestDto;

public interface Policy {
    PolicyEvaluationResult evaluate(TransferRequestDto request);
}
