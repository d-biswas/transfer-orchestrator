package com.company.orchestrator.policy.service.impl;

import com.company.orchestrator.api.request.TransferInitiateDto;
import com.company.orchestrator.policy.model.*;
import com.company.orchestrator.policy.service.PolicyService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PolicyServiceImpl implements PolicyService {

    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final List<Policy> policies = new ArrayList<>();

    @PostConstruct
    public void loadPolicies() throws Exception {
        InputStream is = getClass().getResourceAsStream("/policies.json");
        if (is == null) {
            throw new RuntimeException("policies.json not found in resources!");
        }
        JsonNode root = objectMapper.readTree(is);
        for (JsonNode node : root.get("policies")) {
            String type = node.get("type").asText();
            switch (type) {
                case "TimeBasedPolicy":
                    LocalTime start = LocalTime.parse(node.get("startTime").asText());
                    LocalTime end = LocalTime.parse(node.get("endTime").asText());
                    policies.add(new TimeBasedPolicy(start, end));
                    break;

                case "RateLimitPolicy":
                    int maxRequests = node.get("maxRequestsPerHour").asInt();
                    policies.add(new RateLimitPolicy(maxRequests, redisTemplate));
                    break;

                case "GeographicPolicy":
                    List<String> allowedRegions = new ArrayList<>();
                    for (JsonNode regionNode : node.get("allowedRegions")) {
                        allowedRegions.add(regionNode.asText());
                    }
                    policies.add(new GeographicPolicy(allowedRegions));
                    break;

                case "CertificationPolicy":
                    String requiredCert = node.get("requiredCertification").asText();
                    policies.add(new CertificationPolicy(requiredCert));
                    break;

                case "UsagePolicy":
                    String allowedUsage = node.get("allowedUsage").asText();
                    policies.add(new UsagePolicy(allowedUsage));
                    break;

                default:
                    throw new IllegalArgumentException("Unknown policy type: " + type);
            }
        }
    }

    @Override
    public PolicyEvaluationResult evaluatePolicies(TransferInitiateDto request) {
        AndPolicy all = new AndPolicy(policies);
        return all.evaluate(request);
    }
}
