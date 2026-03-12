package com.chj.aigc.web.dto;

/**
 * 租户负责人启用或停用成员时使用的请求体。
 */
public record UpdateTenantMemberStatusRequest(
        boolean active
) {
}
