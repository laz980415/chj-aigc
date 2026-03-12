package com.chj.aigc.access;

import java.time.Instant;
import java.util.Objects;

public record ModelAccessRule(
        String id,
        String platformModelAlias,
        ModelAccessScope scope,
        ModelAccessEffect effect,
        boolean active,
        String createdBy,
        Instant createdAt,
        String reason
) {
    public ModelAccessRule {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(platformModelAlias, "platformModelAlias");
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(effect, "effect");
        Objects.requireNonNull(createdBy, "createdBy");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(reason, "reason");
    }
}
