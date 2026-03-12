package com.chj.aigc.auth;

import java.util.Objects;

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
