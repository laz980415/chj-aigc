package com.chj.aigc.access;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class ModelAccessPolicyEngine {
    private final List<ModelAccessRule> rules = new ArrayList<>();
    private final List<ModelAccessAuditEvent> auditEvents = new ArrayList<>();

    public void addRule(ModelAccessRule rule) {
        rules.add(Objects.requireNonNull(rule, "rule"));
    }

    public void disableRule(String ruleId, String actorId, String auditEventId) {
        for (int i = 0; i < rules.size(); i++) {
            ModelAccessRule existing = rules.get(i);
            if (existing.id().equals(ruleId)) {
                rules.set(i, new ModelAccessRule(
                        existing.id(),
                        existing.platformModelAlias(),
                        existing.scope(),
                        existing.effect(),
                        false,
                        existing.createdBy(),
                        existing.createdAt(),
                        existing.reason()
                ));
                auditEvents.add(new ModelAccessAuditEvent(
                        auditEventId,
                        actorId,
                        "RULE_DISABLED",
                        existing.platformModelAlias(),
                        existing.scope().type().name(),
                        existing.scope().value(),
                        "Rule disabled",
                        java.time.Instant.now()
                ));
                return;
            }
        }
        throw new IllegalArgumentException("Unknown rule id: " + ruleId);
    }

    public ModelAccessDecision evaluate(ModelAccessRequest request) {
        List<ModelAccessRule> candidates = rules.stream()
                .filter(ModelAccessRule::active)
                .filter(rule -> rule.platformModelAlias().equals(request.platformModelAlias()))
                .filter(rule -> matches(rule.scope(), request))
                .sorted(Comparator.comparingInt(rule -> priority(rule.scope().type())))
                .toList();

        if (candidates.isEmpty()) {
            return new ModelAccessDecision(false, "No active access rule matched", null);
        }

        ModelAccessRule matched = candidates.get(0);
        if (matched.effect() == ModelAccessEffect.DENY) {
            return new ModelAccessDecision(false, "Denied by matching access rule", matched);
        }
        return new ModelAccessDecision(true, "Allowed by matching access rule", matched);
    }

    public ModelAccessAuditEvent recordRuleCreated(String auditEventId, String actorId, ModelAccessRule rule) {
        ModelAccessAuditEvent event = new ModelAccessAuditEvent(
                auditEventId,
                actorId,
                "RULE_CREATED",
                rule.platformModelAlias(),
                rule.scope().type().name(),
                rule.scope().value(),
                rule.reason(),
                java.time.Instant.now()
        );
        auditEvents.add(event);
        return event;
    }

    public List<ModelAccessRule> rules() {
        return List.copyOf(rules);
    }

    public List<ModelAccessAuditEvent> auditEvents() {
        return List.copyOf(auditEvents);
    }

    private boolean matches(ModelAccessScope scope, ModelAccessRequest request) {
        return switch (scope.type()) {
            case TENANT -> scope.value().equals(request.tenantId());
            case PROJECT -> scope.value().equals(request.projectId());
            case ROLE -> request.roleKeys().contains(scope.value());
        };
    }

    private int priority(AccessScopeType type) {
        return switch (type) {
            case PROJECT -> 1;
            case ROLE -> 2;
            case TENANT -> 3;
        };
    }
}
