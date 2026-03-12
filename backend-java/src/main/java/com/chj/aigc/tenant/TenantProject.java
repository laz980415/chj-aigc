package com.chj.aigc.tenant;

public record TenantProject(
        String id,
        String tenantId,
        String name,
        boolean active
) {
}
