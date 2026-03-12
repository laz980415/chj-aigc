package com.chj.aigc.web.dto;

/**
 * 租户负责人调整成员角色时使用的请求体。
 */
public record UpdateTenantMemberRoleRequest(
        String roleKey
) {
}
