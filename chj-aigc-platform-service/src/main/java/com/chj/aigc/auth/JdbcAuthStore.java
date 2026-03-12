package com.chj.aigc.auth;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;

public final class JdbcAuthStore implements AuthStore {
    private final JdbcTemplate jdbcTemplate;

    public JdbcAuthStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<AuthUser> listUsers() {
        return jdbcTemplate.query(
                """
                select id, username, password, display_name, role_key, tenant_id, active
                from auth_users
                order by username asc
                """,
                this::mapUser
        );
    }

    @Override
    public Optional<AuthUser> findUserById(String userId) {
        List<AuthUser> results = jdbcTemplate.query(
                """
                select id, username, password, display_name, role_key, tenant_id, active
                from auth_users
                where id = ?
                """,
                this::mapUser,
                userId
        );
        return results.stream().findFirst();
    }

    @Override
    public Optional<AuthUser> findUserByUsername(String username) {
        List<AuthUser> results = jdbcTemplate.query(
                """
                select id, username, password, display_name, role_key, tenant_id, active
                from auth_users
                where username = ?
                """,
                this::mapUser,
                username
        );
        return results.stream().findFirst();
    }

    @Override
    public void saveUser(AuthUser user) {
        jdbcTemplate.update(
                """
                insert into auth_users (id, username, password, display_name, role_key, tenant_id, active)
                values (?, ?, ?, ?, ?, ?, ?)
                on conflict (id) do update set
                    username = excluded.username,
                    password = excluded.password,
                    display_name = excluded.display_name,
                    role_key = excluded.role_key,
                    tenant_id = excluded.tenant_id,
                    active = excluded.active
                """,
                user.id(),
                user.username(),
                user.password(),
                user.displayName(),
                user.roleKey(),
                user.tenantId(),
                user.active()
        );
    }

    @Override
    public Optional<AuthSession> findSessionByToken(String token) {
        List<AuthSession> results = jdbcTemplate.query(
                """
                select token, user_id, username, display_name, role_key, tenant_id, created_at, expires_at
                from auth_sessions
                where token = ?
                """,
                this::mapSession,
                token
        );
        return results.stream().findFirst();
    }

    @Override
    public void saveSession(AuthSession session) {
        jdbcTemplate.update(
                """
                insert into auth_sessions (token, user_id, username, display_name, role_key, tenant_id, created_at, expires_at)
                values (?, ?, ?, ?, ?, ?, ?, ?)
                on conflict (token) do update set
                    user_id = excluded.user_id,
                    username = excluded.username,
                    display_name = excluded.display_name,
                    role_key = excluded.role_key,
                    tenant_id = excluded.tenant_id,
                    created_at = excluded.created_at,
                    expires_at = excluded.expires_at
                """,
                session.token(),
                session.userId(),
                session.username(),
                session.displayName(),
                session.roleKey(),
                session.tenantId(),
                Timestamp.from(session.createdAt()),
                Timestamp.from(session.expiresAt())
        );
    }

    private AuthUser mapUser(ResultSet resultSet, int rowNum) throws SQLException {
        return new AuthUser(
                resultSet.getString("id"),
                resultSet.getString("username"),
                resultSet.getString("password"),
                resultSet.getString("display_name"),
                resultSet.getString("role_key"),
                resultSet.getString("tenant_id"),
                resultSet.getBoolean("active")
        );
    }

    private AuthSession mapSession(ResultSet resultSet, int rowNum) throws SQLException {
        return new AuthSession(
                resultSet.getString("token"),
                resultSet.getString("user_id"),
                resultSet.getString("username"),
                resultSet.getString("display_name"),
                resultSet.getString("role_key"),
                resultSet.getString("tenant_id"),
                resultSet.getTimestamp("created_at").toInstant(),
                resultSet.getTimestamp("expires_at").toInstant()
        );
    }
}
