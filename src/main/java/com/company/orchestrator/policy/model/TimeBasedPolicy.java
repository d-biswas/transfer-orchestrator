package com.company.orchestrator.policy.model;

import com.company.orchestrator.domain.model.TransferRequest;
import lombok.RequiredArgsConstructor;

import java.time.LocalTime;
import java.time.ZoneId;

@RequiredArgsConstructor
public class TimeBasedPolicy implements Policy {

    private final LocalTime startTime;
    private final LocalTime endTime;

    @Override
    public PolicyEvaluationResult evaluate(TransferRequest request) {
        LocalTime now = LocalTime.now(ZoneId.of("CET"));
        boolean allowed = !now.isBefore(startTime) && !now.isAfter(endTime);
        return new PolicyEvaluationResult(this, allowed,
            allowed ? null : "Transfer not allowed outside business hours");
    }
}
