package com.company.orchestrator.api.response;

import lombok.*;

/**
 * Policy evaluation response DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyEvaluationResponse {
    private boolean allowed;
    private String violationReason;
}
