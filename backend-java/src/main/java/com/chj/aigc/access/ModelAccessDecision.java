package com.chj.aigc.access;

import java.util.Objects;
import java.util.Optional;

public record ModelAccessDecision(
        boolean allowed,
        String reason,
        ModelAccessRule matchedRule
) {
    public ModelAccessDecision {
        Objects.requireNonNull(reason, "reason");
    }

    public Optional<ModelAccessRule> matchedRuleOptional() {
        return Optional.ofNullable(matchedRule);
    }
}
