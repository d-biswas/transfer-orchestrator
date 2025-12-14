package com.company.orchestrator.policy.service;

import com.company.orchestrator.api.request.TransferInitiateDto;
import com.company.orchestrator.policy.model.PolicyEvaluationResult;


public interface PolicyService {
    PolicyEvaluationResult evaluatePolicies(TransferInitiateDto request);

}
