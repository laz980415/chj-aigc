package com.chj.aigc.auth;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 负责平台登录、会话签发以及账号创建。
 * 当前系统同时服务平台超管和租户侧账号，因此这里统一维护账号和角色的基础能力。
 */
public final class AuthService implements PlatformAuthService {
    private final AuthStore authStore;

    public AuthService(AuthStore authStore) {
        this.authStore = authStore;
        seedUsersIfEmpty();
    }

    /**
     * 根据用户名密码完成登录，并返回 12 小时有效的会话令牌。
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
     * 返回当前所有账号，供平台超管页和租户成员页复用。
     */
    public List<AuthUser> listUsers() {
        return authStore.listUsers();
    }

    /**
     * 返回指定租户下的成员账号，平台账号不会出现在这个列表里。
     */
    public List<AuthUser> listTenantUsers(String tenantId) {
        return authStore.listUsers().stream()
                .filter(user -> tenantId.equals(user.tenantId()))
                .filter(user -> List.of("tenant_owner", "tenant_member").contains(user.roleKey()))
                .toList();
    }

    /**
     * 创建任意角色账号，主要给平台超管使用。
     */
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

    /**
     * 创建租户内部成员，只允许租户负责人使用，角色限制为 tenant_owner 或 tenant_member。
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
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("租户成员必须绑定租户");
        }
        return createUser(userId, username, password, displayName, roleKey, tenantId);
    }

    /**
     * 更新租户成员启用状态。
     * 只允许处理当前租户下的租户角色账号，避免平台超管或其他租户账号被误修改。
     */
    public AuthUser updateTenantUserStatus(String userId, String tenantId, boolean active) {
        AuthUser existing = authStore.findUserById(userId)
                .orElseThrow(() -> new IllegalArgumentException("成员不存在"));
        if (!tenantId.equals(existing.tenantId())) {
            throw new IllegalArgumentException("只能修改当前租户下的成员");
        }
        if (!List.of("tenant_owner", "tenant_member").contains(existing.roleKey())) {
            throw new IllegalArgumentException("只能修改租户成员状态");
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
     * 调整租户成员角色，只允许在 tenant_owner 和 tenant_member 之间切换。
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
        if (!List.of("tenant_owner", "tenant_member").contains(existing.roleKey())) {
            throw new IllegalArgumentException("只能修改租户成员角色");
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
        authStore.saveUser(new AuthUser(
                "user-demo",
                "tenant_member",
                "Member@123",
                "租户成员",
                "tenant_member",
                "tenant-demo",
                true
        ));
    }
}
