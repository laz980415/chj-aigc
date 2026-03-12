package com.chj.aigc.tenantservice.web.dto;

/**
 * 修改成员角色请求体。
 */
public record UpdateTenantMemberRoleRequest(
        String roleKey
) {
}
