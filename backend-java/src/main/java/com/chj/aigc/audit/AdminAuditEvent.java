package com.chj.aigc.audit;

import java.time.Instant;
import java.util.Objects;

public record AdminAuditEvent(
        String id,
        String actorId,
        String action,
        String resourceType,
        String resourceId,
        String detail,
        Instant createdAt
) {
    public AdminAuditEvent {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(resourceType, "resourceType");
        Objects.requireNonNull(resourceId, "resourceId");
        Objects.requireNonNull(detail, "detail");
        Objects.requireNonNull(createdAt, "createdAt");
    }
}
