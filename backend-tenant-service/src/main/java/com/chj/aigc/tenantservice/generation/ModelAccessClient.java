package com.chj.aigc.tenantservice.generation;

import java.util.Set;

/**
 * 平台模型访问策略校验客户端。
 */
public interface ModelAccessClient {
    ModelAccessDecision evaluate(String tenantId, String projectId, Set<String> roleKeys, String modelAlias);

    record ModelAccessDecision(
            boolean allowed,
            String reason,
            String matchedRuleId
    ) {
    }
}
