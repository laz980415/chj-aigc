package com.chj.aigc.authservice.auth;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 认证服务登录、会话查询与账号管理。
 * 认证服务是 auth_users 和 auth_sessions 表的唯一拥有者，
 * 平台服务和租户服务通过 HTTP 接口访问账号数据，不直接查询这两张表。
 */
public class AuthService {
    private final AuthStore authStore;

    public AuthService(AuthStore authStore) {
        this.authStore = authStore;
        seedUsersIfEmpty();
    }

    /** 按用户名密码登录，创建 12 小时有效会话。 */
    public AuthSession login(String username, String password) {
        AuthUser user = authStore.findUserByUsername(username)
                .filter(AuthUser::active)
                .filter(u -> u.password().equals(password))
                .orElseThrow(() -> new IllegalArgumentException("用户名或密码错误"));

        Instant now = Instant.now();
        AuthSession session = new AuthSession(
                UUID.randomUUID().toString(),
                user.id(), user.username(), user.displayName(),
                user.roleKey(), user.tenantId(),
                now, now.plus(12, ChronoUnit.HOURS)
        );
        authStore.saveSession(session);
        return session;
    }

    /** 根据 token 查询有效会话。 */
    public Optional<AuthSession> findSession(String token) {
        return authStore.findSessionByToken(token)
                .filter(s -> s.expiresAt().isAfter(Instant.now()));
    }

    /** 返回所有账号，供平台超管使用。 */
    public List<AuthUser> listUsers() {
        return authStore.listUsers();
    }

    /** 返回指定租户下的成员账号。 */
    public List<AuthUser> listTenantUsers(String tenantId) {
        return authStore.listUsersByTenantId(tenantId).stream()
                .filter(u -> List.of("tenant_owner", "tenant_member").contains(u.roleKey()))
                .toList();
    }

    /** 创建任意角色账号，主要给平台超管使用。 */
    public AuthUser createUser(String userId, String username, String password,
                               String displayName, String roleKey, String tenantId) {
        AuthUser user = new AuthUser(userId, username, password, displayName, roleKey, tenantId, true);
        authStore.saveUser(user);
        return user;
    }

    /** 创建租户内部成员，角色限制为 tenant_owner 或 tenant_member。 */
    public AuthUser createTenantUser(String userId, String username, String password,
                                     String displayName, String roleKey, String tenantId) {
        if (!List.of("tenant_owner", "tenant_member").contains(roleKey)) {
            throw new IllegalArgumentException("租户成员角色只允许 tenant_owner 或 tenant_member");
        }
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("租户成员必须绑定租户");
        }
        return createUser(userId, username, password, displayName, roleKey, tenantId);
    }

    /** 更新租户成员启用状态。 */
    public AuthUser updateUserStatus(String userId, String tenantId, boolean active) {
        AuthUser existing = authStore.findUserById(userId)
                .orElseThrow(() -> new IllegalArgumentException("成员不存在"));
        if (!tenantId.equals(existing.tenantId())) {
            throw new IllegalArgumentException("只能修改当前租户下的成员");
        }
        AuthUser updated = new AuthUser(existing.id(), existing.username(), existing.password(),
                existing.displayName(), existing.roleKey(), existing.tenantId(), active);
        authStore.saveUser(updated);
        return updated;
    }

    /** 调整租户成员角色。 */
    public AuthUser updateUserRole(String userId, String tenantId, String roleKey) {
        if (!List.of("tenant_owner", "tenant_member").contains(roleKey)) {
            throw new IllegalArgumentException("租户成员角色只允许 tenant_owner 或 tenant_member");
        }
        AuthUser existing = authStore.findUserById(userId)
                .orElseThrow(() -> new IllegalArgumentException("成员不存在"));
        if (!tenantId.equals(existing.tenantId())) {
            throw new IllegalArgumentException("只能修改当前租户下的成员");
        }
        AuthUser updated = new AuthUser(existing.id(), existing.username(), existing.password(),
                existing.displayName(), roleKey, existing.tenantId(), existing.active());
        authStore.saveUser(updated);
        return updated;
    }

    public List<String> builtinRoles() {
        return List.of("platform_super_admin", "tenant_owner", "tenant_member", "project_admin", "project_user");
    }

    private void seedUsersIfEmpty() {
        if (!authStore.listUsers().isEmpty()) return;
        authStore.saveUser(new AuthUser("user-super-admin", "admin", "Admin@123", "平台超管", "platform_super_admin", null, true));
        authStore.saveUser(new AuthUser("user-tenant-owner", "tenant_owner", "Tenant@123", "租户负责人", "tenant_owner", "tenant-demo", true));
        authStore.saveUser(new AuthUser("user-demo", "tenant_member", "Member@123", "租户成员", "tenant_member", "tenant-demo", true));
    }
}
