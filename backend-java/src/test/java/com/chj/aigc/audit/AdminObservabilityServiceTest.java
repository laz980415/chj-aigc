package com.chj.aigc.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AdminObservabilityServiceTest {

    @Test
    void adminActionsAndSafetyIncidentsAreRecorded() {
        AdminObservabilityService service = new AdminObservabilityService();
        service.recordAdminAction(
                "audit-1",
                "super-admin",
                "MODEL_RULE_UPDATED",
                "model_access_rule",
                "rule-1",
                "Disabled video access for project-2"
        );
        service.addSafetyPolicy(new SafetyPolicy(
                "policy-1",
                "tenant-1",
                "Forbidden medical claims",
                "cure acne permanently",
                true
        ));
        service.recordSafetyIncident(
                "incident-1",
                "tenant-1",
                "project-1",
                "job-1",
                "user-1",
                "policy-1",
                "Output matched forbidden medical claims policy"
        );

        assertEquals(1, service.adminEvents().size());
        assertEquals(1, service.safetyPolicies().size());
        assertEquals(1, service.safetyIncidents().size());
    }
}
