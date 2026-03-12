package com.chj.aigc.audit;

import java.util.Objects;

public record SafetyPolicy(
        String id,
        String tenantId,
        String name,
        String forbiddenTerm,
        boolean active
) {
    public SafetyPolicy {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(forbiddenTerm, "forbiddenTerm");
    }
}
