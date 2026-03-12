package com.chj.aigc.access;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ModelAccessPolicyEngineTest {

    @Test
    void projectScopedDenyOverridesTenantAllow() {
        ModelAccessPolicyEngine engine = new ModelAccessPolicyEngine();
        ModelAccessRule tenantAllow = new ModelAccessRule(
                "rule-1",
                "image-standard",
                new ModelAccessScope(AccessScopeType.TENANT, "tenant-1"),
                ModelAccessEffect.ALLOW,
                true,
                "super-admin",
                Instant.now(),
                "Allow image model for tenant"
        );
        ModelAccessRule projectDeny = new ModelAccessRule(
                "rule-2",
                "image-standard",
                new ModelAccessScope(AccessScopeType.PROJECT, "project-2"),
                ModelAccessEffect.DENY,
                true,
                "super-admin",
                Instant.now(),
                "Block expensive image model in project-2"
        );
        engine.addRule(tenantAllow);
        engine.addRule(projectDeny);

        ModelAccessDecision decision = engine.evaluate(new ModelAccessRequest(
                "tenant-1",
                "project-2",
                Set.of("project_user"),
                "image-standard"
        ));

        assertFalse(decision.allowed());
        assertEquals("rule-2", decision.matchedRule().id());
    }

    @Test
    void roleBasedAllowCanGrantModelUse() {
        ModelAccessPolicyEngine engine = new ModelAccessPolicyEngine();
        ModelAccessRule roleAllow = new ModelAccessRule(
                "rule-3",
                "video-standard",
                new ModelAccessScope(AccessScopeType.ROLE, "project_admin"),
                ModelAccessEffect.ALLOW,
                true,
                "super-admin",
                Instant.now(),
                "Only project admins may use video"
        );
        engine.addRule(roleAllow);

        ModelAccessDecision decision = engine.evaluate(new ModelAccessRequest(
                "tenant-1",
                "project-1",
                Set.of("project_admin"),
                "video-standard"
        ));

        assertTrue(decision.allowed());
        assertEquals("rule-3", decision.matchedRule().id());
    }

    @Test
    void missingRuleDeniesByDefault() {
        ModelAccessPolicyEngine engine = new ModelAccessPolicyEngine();

        ModelAccessDecision decision = engine.evaluate(new ModelAccessRequest(
                "tenant-1",
                "project-1",
                Set.of("project_user"),
                "copy-standard"
        ));

        assertFalse(decision.allowed());
        assertEquals("No active access rule matched", decision.reason());
    }

    @Test
    void ruleChangesAreAuditable() {
        ModelAccessPolicyEngine engine = new ModelAccessPolicyEngine();
        ModelAccessRule roleAllow = new ModelAccessRule(
                "rule-4",
                "copy-standard",
                new ModelAccessScope(AccessScopeType.ROLE, "tenant_owner"),
                ModelAccessEffect.ALLOW,
                true,
                "super-admin",
                Instant.now(),
                "Tenant owners may use copy models"
        );

        engine.addRule(roleAllow);
        engine.recordRuleCreated("audit-1", "super-admin", roleAllow);
        engine.disableRule("rule-4", "super-admin", "audit-2");

        assertEquals(2, engine.auditEvents().size());
        assertEquals("RULE_CREATED", engine.auditEvents().get(0).action());
        assertEquals("RULE_DISABLED", engine.auditEvents().get(1).action());
    }
}
