package com.chj.aigc.tenantservice.web.dto;

/**
 * 创建租户成员请求体。
 */
public record CreateTenantMemberRequest(
        String userId,
        String username,
        String password,
        String displayName,
        String roleKey
) {
}
