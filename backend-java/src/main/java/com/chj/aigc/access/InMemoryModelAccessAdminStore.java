package com.chj.aigc.access;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class InMemoryModelAccessAdminStore implements ModelAccessAdminStore {
    private final List<ModelAccessRule> rules = new ArrayList<>();
    private final List<ModelAccessAuditEvent> auditEvents = new ArrayList<>();

    @Override
    public List<ModelAccessRule> listRules() {
        return List.copyOf(rules);
    }

    @Override
    public List<ModelAccessAuditEvent> listAuditEvents() {
        return List.copyOf(auditEvents);
    }

    @Override
    public void saveRule(ModelAccessRule rule) {
        rules.add(Objects.requireNonNull(rule, "rule"));
    }

    @Override
    public void saveAuditEvent(ModelAccessAuditEvent event) {
        auditEvents.add(Objects.requireNonNull(event, "event"));
    }
}
