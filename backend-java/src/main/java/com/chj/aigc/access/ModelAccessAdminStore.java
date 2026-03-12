package com.chj.aigc.access;

import java.util.List;

public interface ModelAccessAdminStore {
    List<ModelAccessRule> listRules();

    List<ModelAccessAuditEvent> listAuditEvents();

    void saveRule(ModelAccessRule rule);

    void saveAuditEvent(ModelAccessAuditEvent event);
}
