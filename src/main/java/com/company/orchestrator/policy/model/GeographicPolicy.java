package com.company.orchestrator.policy.model;

import com.company.orchestrator.api.dto.TransferRequestDto;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class GeographicPolicy implements Policy {

    private final List<String> allowedRegions;

    @Override
    public PolicyEvaluationResult evaluate(TransferRequestDto request) {
        boolean allowed = allowedRegions.contains(request.getConsumerRegion());
        return new PolicyEvaluationResult(this, allowed,
            allowed ? null : "Data cannot leave allowed regions");
    }
}
