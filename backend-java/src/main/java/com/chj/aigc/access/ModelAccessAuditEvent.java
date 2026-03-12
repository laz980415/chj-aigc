package com.chj.aigc.access;

import java.time.Instant;
import java.util.Objects;

public record ModelAccessAuditEvent(
        String id,
        String actorId,
        String action,
        String targetModelAlias,
        String targetScopeType,
        String targetScopeValue,
        String detail,
        Instant createdAt
) {
    public ModelAccessAuditEvent {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(targetModelAlias, "targetModelAlias");
        Objects.requireNonNull(targetScopeType, "targetScopeType");
        Objects.requireNonNull(targetScopeValue, "targetScopeValue");
        Objects.requireNonNull(detail, "detail");
        Objects.requireNonNull(createdAt, "createdAt");
    }
}
