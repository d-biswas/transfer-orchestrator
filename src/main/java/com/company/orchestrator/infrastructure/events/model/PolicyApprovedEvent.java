package com.company.orchestrator.infrastructure.events.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.io.Serial;

/**
 * Event fired when policy evaluation approves a transfer
 * Topic: policy.approved
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class PolicyApprovedEvent extends BaseTransferEvent {
    @Serial
    private static final long serialVersionUID = 1L;

    private String policyType;

    @Override
    public String getEventType() {
        return "policy.approved";
    }
}