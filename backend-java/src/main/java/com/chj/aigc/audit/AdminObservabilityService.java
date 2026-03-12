package com.chj.aigc.audit;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class AdminObservabilityService {
    private final List<AdminAuditEvent> adminEvents = new ArrayList<>();
    private final List<SafetyPolicy> safetyPolicies = new ArrayList<>();
    private final List<SafetyIncident> safetyIncidents = new ArrayList<>();

    public AdminAuditEvent recordAdminAction(
            String eventId,
            String actorId,
            String action,
            String resourceType,
            String resourceId,
            String detail
    ) {
        AdminAuditEvent event = new AdminAuditEvent(
                eventId,
                actorId,
                action,
                resourceType,
                resourceId,
                detail,
                Instant.now()
        );
        adminEvents.add(event);
        return event;
    }

    public void addSafetyPolicy(SafetyPolicy policy) {
        safetyPolicies.add(policy);
    }

    public SafetyIncident recordSafetyIncident(
            String incidentId,
            String tenantId,
            String projectId,
            String jobId,
            String actorId,
            String policyId,
            String message
    ) {
        SafetyIncident incident = new SafetyIncident(
                incidentId,
                tenantId,
                projectId,
                jobId,
                actorId,
                policyId,
                message,
                Instant.now()
        );
        safetyIncidents.add(incident);
        return incident;
    }

    public List<AdminAuditEvent> adminEvents() {
        return List.copyOf(adminEvents);
    }

    public List<SafetyPolicy> safetyPolicies() {
        return List.copyOf(safetyPolicies);
    }

    public List<SafetyIncident> safetyIncidents() {
        return List.copyOf(safetyIncidents);
    }
}
