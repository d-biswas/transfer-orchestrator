package com.company.orchestrator.policy.model;

import com.company.orchestrator.domain.model.TransferRequest;

import java.time.Instant;
import java.time.Duration;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.Map;

public class RateLimitPolicy implements Policy {

    private final int maxRequests;
    private final Duration window; // e.g., 1 hour

    // Key = consumerId, Value = timestamps of requests
    private final Map<String, Deque<Instant>> requestLogs = new ConcurrentHashMap<>();

    public RateLimitPolicy(int maxRequestsPerHour) {
        this.maxRequests = maxRequestsPerHour;
        this.window = Duration.ofHours(1);
    }

    @Override
    public PolicyEvaluationResult evaluate(TransferRequest request) {
        String consumerId = request.getConsumerId();
        Instant now = request.getRequestTime().atZone(java.time.ZoneId.systemDefault()).toInstant();

        Deque<Instant> timestamps = requestLogs.computeIfAbsent(consumerId, k -> new ConcurrentLinkedDeque<>());

        // Remove timestamps older than the window
        while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(now.minus(window))) {
            timestamps.pollFirst();
        }

        boolean allowed = timestamps.size() < maxRequests;
        if (allowed) {
            timestamps.addLast(now);
        }

        return new PolicyEvaluationResult(this, allowed,
                allowed ? null : "Rate limit exceeded for consumer " + consumerId);
    }
}
