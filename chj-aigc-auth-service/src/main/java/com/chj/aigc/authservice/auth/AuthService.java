package com.chj.aigc.authservice.auth;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

/**
 * 认证服务登录与会话查询。
 */
public class AuthService {
    private final AuthStore authStore;

    public AuthService(AuthStore authStore) {
        this.authStore = authStore;
    }

    /**
     * 按用户名密码登录，创建 12 小时有效会话。
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

    /**
     * 根据 token 查询有效会话。
     */
    public Optional<AuthSession> findSession(String token) {
        return authStore.findSessionByToken(token)
                .filter(session -> session.expiresAt().isAfter(Instant.now()));
    }
}
