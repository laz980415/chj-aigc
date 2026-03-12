package com.chj.aigc.audit;

import java.time.Instant;
import java.util.Objects;

public record SafetyIncident(
        String id,
        String tenantId,
        String projectId,
        String jobId,
        String actorId,
        String policyId,
        String message,
        Instant createdAt
) {
    public SafetyIncident {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(projectId, "projectId");
        Objects.requireNonNull(jobId, "jobId");
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(policyId, "policyId");
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(createdAt, "createdAt");
    }
}
