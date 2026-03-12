package com.chj.aigc.tenantservice.auth;

import java.util.Objects;

/**
 * 租户服务账号实体。
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
    public AuthUser {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(password, "password");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(roleKey, "roleKey");
    }
}
