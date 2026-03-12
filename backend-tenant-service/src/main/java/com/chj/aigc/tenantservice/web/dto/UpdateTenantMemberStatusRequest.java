package com.chj.aigc.tenantservice.web.dto;

/**
 * 修改成员状态请求体。
 */
public record UpdateTenantMemberStatusRequest(
        boolean active
) {
}
