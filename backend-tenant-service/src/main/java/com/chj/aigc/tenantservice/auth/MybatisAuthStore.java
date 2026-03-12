package com.chj.aigc.tenantservice.auth;

import com.chj.aigc.tenantservice.persistence.RowValueHelper;
import com.chj.aigc.tenantservice.persistence.mapper.AuthMapper;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 基于 MyBatis XML 的账号与会话存储。
 */
public final class MybatisAuthStore implements AuthStore {
    private final AuthMapper authMapper;

    public MybatisAuthStore(AuthMapper authMapper) {
        this.authMapper = authMapper;
    }

    @Override
    public List<AuthUser> listUsers() {
        return authMapper.listUsers().stream().map(this::mapUser).toList();
    }

    @Override
    public Optional<AuthUser> findUserById(String userId) {
        return Optional.ofNullable(authMapper.findUserById(userId)).map(this::mapUser);
    }

    @Override
    public Optional<AuthUser> findUserByUsername(String username) {
        return Optional.ofNullable(authMapper.findUserByUsername(username)).map(this::mapUser);
    }

    @Override
    public void saveUser(AuthUser user) {
        authMapper.upsertUser(user);
    }

    @Override
    public Optional<AuthSession> findSessionByToken(String token) {
        return Optional.ofNullable(authMapper.findSessionByToken(token)).map(this::mapSession);
    }

    @Override
    public void saveSession(AuthSession session) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("token", session.token());
        payload.put("userId", session.userId());
        payload.put("username", session.username());
        payload.put("displayName", session.displayName());
        payload.put("roleKey", session.roleKey());
        payload.put("tenantId", session.tenantId());
        payload.put("createdAt", Timestamp.from(session.createdAt()));
        payload.put("expiresAt", Timestamp.from(session.expiresAt()));
        authMapper.upsertSession(payload);
    }

    private AuthUser mapUser(Map<String, Object> row) {
        String username = RowValueHelper.string(row, "username");
        String displayName = RowValueHelper.string(row, "displayName", "display_name");
        String roleKey = RowValueHelper.string(row, "roleKey", "role_key");
        return new AuthUser(
                RowValueHelper.string(row, "id"),
                username,
                RowValueHelper.string(row, "password"),
                displayName != null ? displayName : username,
                roleKey != null ? roleKey : "tenant_member",
                RowValueHelper.string(row, "tenantId", "tenant_id"),
                RowValueHelper.bool(row, "active")
        );
    }

    private AuthSession mapSession(Map<String, Object> row) {
        Timestamp createdAt = RowValueHelper.timestamp(row, "createdAt", "created_at");
        Timestamp expiresAt = RowValueHelper.timestamp(row, "expiresAt", "expires_at");
        String username = RowValueHelper.string(row, "username");
        String displayName = RowValueHelper.string(row, "displayName", "display_name");
        String roleKey = RowValueHelper.string(row, "roleKey", "role_key");
        return new AuthSession(
                RowValueHelper.string(row, "token"),
                RowValueHelper.string(row, "userId", "user_id"),
                username,
                displayName != null ? displayName : username,
                roleKey != null ? roleKey : "tenant_member",
                RowValueHelper.string(row, "tenantId", "tenant_id"),
                createdAt.toInstant(),
                expiresAt.toInstant()
        );
    }
}
