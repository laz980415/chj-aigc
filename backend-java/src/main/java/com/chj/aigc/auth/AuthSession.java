package com.chj.aigc.auth;

import java.time.Instant;
import java.util.Objects;

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
    public AuthSession {
        Objects.requireNonNull(token, "token");
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(roleKey, "roleKey");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(expiresAt, "expiresAt");
    }
}
