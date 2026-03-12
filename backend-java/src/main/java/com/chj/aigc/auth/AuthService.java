package com.chj.aigc.auth;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class AuthService {
    private final AuthStore authStore;

    public AuthService(AuthStore authStore) {
        this.authStore = authStore;
        seedUsersIfEmpty();
    }

    public AuthSession login(String username, String password) {
        AuthUser user = authStore.findUserByUsername(username)
                .filter(AuthUser::active)
                .filter(existing -> existing.password().equals(password))
                .orElseThrow(() -> new IllegalArgumentException("用户名或密码错误"));

        Instant now = Instant.now();
        AuthSession session = new AuthSession(
                UUID.randomUUID().toString(),
                user.id(),
                user.username(),
                user.displayName(),
                user.roleKey(),
                user.tenantId(),
                now,
                now.plus(12, ChronoUnit.HOURS)
        );
        authStore.saveSession(session);
        return session;
    }

    public Optional<AuthSession> findSession(String token) {
        return authStore.findSessionByToken(token)
                .filter(session -> session.expiresAt().isAfter(Instant.now()));
    }

    public List<AuthUser> listUsers() {
        return authStore.listUsers();
    }

    public AuthUser createUser(
            String userId,
            String username,
            String password,
            String displayName,
            String roleKey,
            String tenantId
    ) {
        AuthUser user = new AuthUser(
                userId,
                username,
                password,
                displayName,
                roleKey,
                tenantId,
                true
        );
        authStore.saveUser(user);
        return user;
    }

    public List<String> builtinRoles() {
        return List.of(
                "platform_super_admin",
                "tenant_owner",
                "tenant_member",
                "project_admin",
                "project_user"
        );
    }

    private void seedUsersIfEmpty() {
        if (!authStore.listUsers().isEmpty()) {
            return;
        }

        authStore.saveUser(new AuthUser(
                "user-super-admin",
                "admin",
                "Admin@123",
                "平台超管",
                "platform_super_admin",
                null,
                true
        ));
        authStore.saveUser(new AuthUser(
                "user-tenant-owner",
                "tenant_owner",
                "Tenant@123",
                "租户负责人",
                "tenant_owner",
                "tenant-demo",
                true
        ));
    }
}
