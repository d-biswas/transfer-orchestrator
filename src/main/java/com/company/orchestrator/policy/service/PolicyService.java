package com.company.orchestrator.policy.service;

import com.company.orchestrator.domain.model.TransferRequest;
import com.company.orchestrator.policy.model.PolicyEvaluationResult;


public interface PolicyService {
    PolicyEvaluationResult evaluatePolicies(TransferRequest request);

}
