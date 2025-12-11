package com.company.orchestrator.policy.service.impl;

import com.company.orchestrator.api.dto.TransferRequestDto;
import com.company.orchestrator.policy.model.*;
import com.company.orchestrator.policy.service.PolicyService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class PolicyServiceImpl implements PolicyService {

    private final List<Policy> policies = new ArrayList<>();

    @PostConstruct
    public void loadPolicies() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        InputStream is = getClass().getResourceAsStream("/policies.json");
        if (is == null) {
            throw new RuntimeException("policies.json not found in resources!");
        }
        JsonNode root = mapper.readTree(is);
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
                    policies.add(new RateLimitPolicy(maxRequests));
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
    public PolicyEvaluationResult evaluatePolicies(TransferRequestDto request) {
        AndPolicy all = new AndPolicy(policies);
        return all.evaluate(request);
    }
}
