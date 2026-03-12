package com.chj.aigc.access;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;

public final class JdbcModelAccessAdminStore implements ModelAccessAdminStore {
    private final JdbcTemplate jdbcTemplate;

    public JdbcModelAccessAdminStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<ModelAccessRule> listRules() {
        return jdbcTemplate.query(
                """
                select id, platform_model_alias, scope_type, scope_value, effect, active, created_by, created_at, reason
                from model_access_rules
                order by created_at asc
                """,
                this::mapRule
        );
    }

    @Override
    public List<ModelAccessAuditEvent> listAuditEvents() {
        return jdbcTemplate.query(
                """
                select id, actor_id, action, target_model_alias, target_scope_type, target_scope_value, detail, created_at
                from model_access_audit_events
                order by created_at asc
                """,
                this::mapAuditEvent
        );
    }

    @Override
    public void saveRule(ModelAccessRule rule) {
        jdbcTemplate.update(
                """
                insert into model_access_rules (
                    id, platform_model_alias, scope_type, scope_value, effect, active, created_by, created_at, reason
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                on conflict (id) do update set
                    platform_model_alias = excluded.platform_model_alias,
                    scope_type = excluded.scope_type,
                    scope_value = excluded.scope_value,
                    effect = excluded.effect,
                    active = excluded.active,
                    created_by = excluded.created_by,
                    created_at = excluded.created_at,
                    reason = excluded.reason
                """,
                rule.id(),
                rule.platformModelAlias(),
                rule.scope().type().name(),
                rule.scope().value(),
                rule.effect().name(),
                rule.active(),
                rule.createdBy(),
                Timestamp.from(rule.createdAt()),
                rule.reason()
        );
    }

    @Override
    public void saveAuditEvent(ModelAccessAuditEvent event) {
        jdbcTemplate.update(
                """
                insert into model_access_audit_events (
                    id, actor_id, action, target_model_alias, target_scope_type, target_scope_value, detail, created_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?)
                on conflict (id) do nothing
                """,
                event.id(),
                event.actorId(),
                event.action(),
                event.targetModelAlias(),
                event.targetScopeType(),
                event.targetScopeValue(),
                event.detail(),
                Timestamp.from(event.createdAt())
        );
    }

    private ModelAccessRule mapRule(ResultSet resultSet, int rowNum) throws SQLException {
        return new ModelAccessRule(
                resultSet.getString("id"),
                resultSet.getString("platform_model_alias"),
                new ModelAccessScope(
                        AccessScopeType.valueOf(resultSet.getString("scope_type")),
                        resultSet.getString("scope_value")
                ),
                ModelAccessEffect.valueOf(resultSet.getString("effect")),
                resultSet.getBoolean("active"),
                resultSet.getString("created_by"),
                resultSet.getTimestamp("created_at").toInstant(),
                resultSet.getString("reason")
        );
    }

    private ModelAccessAuditEvent mapAuditEvent(ResultSet resultSet, int rowNum) throws SQLException {
        return new ModelAccessAuditEvent(
                resultSet.getString("id"),
                resultSet.getString("actor_id"),
                resultSet.getString("action"),
                resultSet.getString("target_model_alias"),
                resultSet.getString("target_scope_type"),
                resultSet.getString("target_scope_value"),
                resultSet.getString("detail"),
                resultSet.getTimestamp("created_at").toInstant()
        );
    }
}
