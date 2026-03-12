package com.chj.aigc.access;

import com.chj.aigc.persistence.RowValueHelper;
import com.chj.aigc.persistence.mapper.ModelAccessMapper;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

/**
 * 基于 MyBatis XML 的模型访问策略存储实现。
 */
public final class MybatisModelAccessAdminStore implements ModelAccessAdminStore {
    private final ModelAccessMapper mapper;

    public MybatisModelAccessAdminStore(ModelAccessMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<ModelAccessRule> listRules() {
        return mapper.listRules().stream()
                .map(this::mapRule)
                .toList();
    }

    @Override
    public List<ModelAccessAuditEvent> listAuditEvents() {
        return mapper.listAuditEvents().stream()
                .map(this::mapAuditEvent)
                .toList();
    }

    @Override
    public void saveRule(ModelAccessRule rule) {
        mapper.upsertRule(Map.of(
                "id", rule.id(),
                "platformModelAlias", rule.platformModelAlias(),
                "scopeType", rule.scope().type().name(),
                "scopeValue", rule.scope().value(),
                "effect", rule.effect().name(),
                "active", rule.active(),
                "createdBy", rule.createdBy(),
                "createdAt", Timestamp.from(rule.createdAt()),
                "reason", rule.reason()
        ));
    }

    @Override
    public void saveAuditEvent(ModelAccessAuditEvent event) {
        mapper.insertAuditEvent(Map.of(
                "id", event.id(),
                "actorId", event.actorId(),
                "action", event.action(),
                "targetModelAlias", event.targetModelAlias(),
                "targetScopeType", event.targetScopeType(),
                "targetScopeValue", event.targetScopeValue(),
                "detail", event.detail(),
                "createdAt", Timestamp.from(event.createdAt())
        ));
    }

    private ModelAccessRule mapRule(Map<String, Object> row) {
        return new ModelAccessRule(
                RowValueHelper.string(row, "id"),
                RowValueHelper.string(row, "platformModelAlias", "platform_model_alias"),
                new ModelAccessScope(
                        AccessScopeType.valueOf(RowValueHelper.string(row, "scopeType", "scope_type")),
                        RowValueHelper.string(row, "scopeValue", "scope_value")
                ),
                ModelAccessEffect.valueOf(RowValueHelper.string(row, "effect")),
                RowValueHelper.bool(row, "active"),
                RowValueHelper.string(row, "createdBy", "created_by"),
                RowValueHelper.timestamp(row, "createdAt", "created_at").toInstant(),
                RowValueHelper.string(row, "reason")
        );
    }

    private ModelAccessAuditEvent mapAuditEvent(Map<String, Object> row) {
        return new ModelAccessAuditEvent(
                RowValueHelper.string(row, "id"),
                RowValueHelper.string(row, "actorId", "actor_id"),
                RowValueHelper.string(row, "action"),
                RowValueHelper.string(row, "targetModelAlias", "target_model_alias"),
                RowValueHelper.string(row, "targetScopeType", "target_scope_type"),
                RowValueHelper.string(row, "targetScopeValue", "target_scope_value"),
                RowValueHelper.string(row, "detail"),
                RowValueHelper.timestamp(row, "createdAt", "created_at").toInstant()
        );
    }
}
