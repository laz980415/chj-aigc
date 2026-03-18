package com.chj.aigc.authservice.auth;

import com.chj.aigc.authservice.persistence.RowValueHelper;
import com.chj.aigc.authservice.persistence.mapper.AuthMapper;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 基于 MyBatis XML 的认证服务存储实现。
 */
public class MybatisAuthStore implements AuthStore {
    private final AuthMapper authMapper;

    public MybatisAuthStore(AuthMapper authMapper) {
        this.authMapper = authMapper;
    }

    @Override
    public Optional<AuthUser> findUserByUsername(String username) {
        return Optional.ofNullable(authMapper.findUserByUsername(username)).map(this::mapUser);
    }

    @Override
    public Optional<AuthUser> findUserById(String id) {
        return Optional.ofNullable(authMapper.findUserById(id)).map(this::mapUser);
    }

    @Override
    public List<AuthUser> listUsers() {
        return authMapper.listUsers().stream().map(this::mapUser).toList();
    }

    @Override
    public List<AuthUser> listUsersByTenantId(String tenantId) {
        return authMapper.listUsersByTenantId(tenantId).stream().map(this::mapUser).toList();
    }

    @Override
    public void saveUser(AuthUser user) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", user.id());
        payload.put("username", user.username());
        payload.put("password", user.password());
        payload.put("displayName", user.displayName());
        payload.put("roleKey", user.roleKey());
        payload.put("tenantId", user.tenantId());
        payload.put("active", user.active());
        authMapper.upsertUser(payload);
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
        String displayName = RowValueHelper.string(row, "displayName", "display_name", "displayname");
        String roleKey = RowValueHelper.string(row, "roleKey", "role_key", "rolekey");
        return new AuthUser(
                RowValueHelper.string(row, "id"),
                username,
                RowValueHelper.string(row, "password"),
                displayName != null ? displayName : username,
                roleKey != null ? roleKey : "tenant_member",
                RowValueHelper.string(row, "tenantId", "tenant_id", "tenantid"),
                RowValueHelper.bool(row, "active")
        );
    }

    private AuthSession mapSession(Map<String, Object> row) {
        Timestamp createdAt = RowValueHelper.timestamp(row, "createdAt", "created_at", "createdat");
        Timestamp expiresAt = RowValueHelper.timestamp(row, "expiresAt", "expires_at", "expiresat");
        Instant createdAtValue = createdAt == null ? Instant.now() : createdAt.toInstant();
        Instant expiresAtValue = expiresAt == null ? createdAtValue.plusSeconds(12 * 60 * 60) : expiresAt.toInstant();
        String username = RowValueHelper.string(row, "username");
        String displayName = RowValueHelper.string(row, "displayName", "display_name", "displayname");
        String roleKey = RowValueHelper.string(row, "roleKey", "role_key", "rolekey");
        return new AuthSession(
                RowValueHelper.string(row, "token"),
                RowValueHelper.string(row, "userId", "userid", "user_id"),
                username,
                displayName != null ? displayName : username,
                roleKey != null ? roleKey : "tenant_member",
                RowValueHelper.string(row, "tenantId", "tenant_id", "tenantid"),
                createdAtValue,
                expiresAtValue
        );
    }
}
