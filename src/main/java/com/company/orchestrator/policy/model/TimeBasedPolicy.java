package com.company.orchestrator.policy.model;

import com.company.orchestrator.api.request.TransferInitiateDto;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;

@RequiredArgsConstructor
public class TimeBasedPolicy implements Policy {

    private final LocalTime startTime;
    private final LocalTime endTime;

    @Override
    public PolicyEvaluationResult evaluate(TransferInitiateDto request) {
        // Use current time for policy evaluation
        LocalTime requestedTime = Instant.now()
                .atZone(ZoneId.of("UTC"))
                .toLocalTime();
        boolean allowed = !requestedTime.isBefore(startTime) && !requestedTime.isAfter(endTime);
        return new PolicyEvaluationResult(this, allowed,
            allowed ? null : "Transfer not allowed outside business hours");
    }
}
