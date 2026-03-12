package com.chj.aigc.tenantservice.tenant;

/**
 * 租户项目实体。
 */
public record TenantProject(
        String id,
        String tenantId,
        String name,
        boolean active
) {
}
