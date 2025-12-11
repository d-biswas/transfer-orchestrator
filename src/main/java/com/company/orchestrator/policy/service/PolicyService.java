package com.company.orchestrator.policy.service;

import com.company.orchestrator.api.dto.TransferRequestDto;
import com.company.orchestrator.policy.model.PolicyEvaluationResult;


public interface PolicyService {
    PolicyEvaluationResult evaluatePolicies(TransferRequestDto request);

}
