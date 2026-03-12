package com.chj.aigc.authservice.auth;

/**
 * 认证服务账号实体。
 */
public record AuthUser(
        String id,
        String username,
        String password,
        String displayName,
        String roleKey,
        String tenantId,
        boolean active
) {
}
