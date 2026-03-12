package com.chj.aigc.persistence.mapper;

import java.util.List;
import java.util.Map;

/**
 * 平台模型访问规则与审计事件的 MyBatis 映射接口。
 */
public interface ModelAccessMapper {
    List<Map<String, Object>> listRules();

    List<Map<String, Object>> listAuditEvents();

    void upsertRule(Map<String, Object> rule);

    void insertAuditEvent(Map<String, Object> event);
}
