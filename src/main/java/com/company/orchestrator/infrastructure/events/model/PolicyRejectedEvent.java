package com.company.orchestrator.infrastructure.events.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.io.Serial;

/**
 * Event fired when policy evaluation rejects a transfer
 * Topic: policy.rejected
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class PolicyRejectedEvent extends BaseTransferEvent {
    @Serial
    private static final long serialVersionUID = 1L;

    private String policyType;
    private String violationReason;

    @Override
    public String getEventType() {
        return "policy.rejected";
    }
}