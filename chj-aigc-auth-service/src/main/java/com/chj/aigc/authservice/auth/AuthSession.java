package com.chj.aigc.authservice.auth;

import java.time.Instant;

/**
 * 认证服务会话实体。
 */
public record AuthSession(
        String token,
        String userId,
        String username,
        String displayName,
        String roleKey,
        String tenantId,
        Instant createdAt,
        Instant expiresAt
) {
}
