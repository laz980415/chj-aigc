package com.chj.aigc.web.dto;

/**
 * 租户负责人创建成员时提交的请求体。
 */
public record CreateTenantMemberRequest(
        String userId,
        String username,
        String password,
        String displayName,
        String roleKey
) {
}
