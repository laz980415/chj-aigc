package com.chj.aigc.tenantservice.auth;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 租户服务登录与成员维护。
 */
public final class AuthService {
    private final AuthStore authStore;

    public AuthService(AuthStore authStore) {
        this.authStore = authStore;
        ensureDefaultUsers();
    }

    /**
     * 根据用户名密码完成登录，并返回 12 小时有效会话。
     */
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

    /**
     * 返回指定租户下成员。
     */
    public List<AuthUser> listTenantUsers(String tenantId) {
        return authStore.listUsers().stream()
                .filter(user -> tenantId.equals(user.tenantId()))
                .filter(user -> List.of("tenant_owner", "tenant_member").contains(user.roleKey()))
                .toList();
    }

    /**
     * 创建租户成员。
     */
    public AuthUser createTenantUser(
            String userId,
            String username,
            String password,
            String displayName,
            String roleKey,
            String tenantId
    ) {
        if (!List.of("tenant_owner", "tenant_member").contains(roleKey)) {
            throw new IllegalArgumentException("租户成员角色只允许 tenant_owner 或 tenant_member");
        }
        AuthUser user = new AuthUser(userId, username, password, displayName, roleKey, tenantId, true);
        authStore.saveUser(user);
        return user;
    }

    /**
     * 修改租户成员状态。
     */
    public AuthUser updateTenantUserStatus(String userId, String tenantId, boolean active) {
        AuthUser existing = authStore.findUserById(userId)
                .orElseThrow(() -> new IllegalArgumentException("成员不存在"));
        if (!tenantId.equals(existing.tenantId())) {
            throw new IllegalArgumentException("只能修改当前租户下的成员");
        }
        AuthUser updated = new AuthUser(
                existing.id(),
                existing.username(),
                existing.password(),
                existing.displayName(),
                existing.roleKey(),
                existing.tenantId(),
                active
        );
        authStore.saveUser(updated);
        return updated;
    }

    /**
     * 在 tenant_owner 与 tenant_member 间切换。
     */
    public AuthUser updateTenantUserRole(String userId, String tenantId, String roleKey) {
        if (!List.of("tenant_owner", "tenant_member").contains(roleKey)) {
            throw new IllegalArgumentException("租户成员角色只允许 tenant_owner 或 tenant_member");
        }
        AuthUser existing = authStore.findUserById(userId)
                .orElseThrow(() -> new IllegalArgumentException("成员不存在"));
        if (!tenantId.equals(existing.tenantId())) {
            throw new IllegalArgumentException("只能修改当前租户下的成员");
        }
        AuthUser updated = new AuthUser(
                existing.id(),
                existing.username(),
                existing.password(),
                existing.displayName(),
                roleKey,
                existing.tenantId(),
                existing.active()
        );
        authStore.saveUser(updated);
        return updated;
    }

    private void ensureDefaultUsers() {
        authStore.saveUser(new AuthUser(
                "user-tenant-owner",
                "tenant_owner",
                "Tenant@123",
                "租户负责人",
                "tenant_owner",
                "tenant-demo",
                true
        ));
        authStore.saveUser(new AuthUser(
                "user-tenant-member",
                "tenant_member",
                "Member@123",
                "租户成员",
                "tenant_member",
                "tenant-demo",
                true
        ));
    }
}
