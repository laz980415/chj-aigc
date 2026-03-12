package com.chj.aigc.tenant;

import com.chj.aigc.persistence.RowValueHelper;
import com.chj.aigc.persistence.mapper.TenantProjectMapper;
import java.util.List;
import java.util.Map;

/**
 * 基于 MyBatis XML 的租户项目存储实现。
 */
public final class MybatisTenantProjectStore implements TenantProjectStore {
    private final TenantProjectMapper mapper;

    public MybatisTenantProjectStore(TenantProjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<TenantProject> listProjects(String tenantId) {
        return mapper.listProjects(tenantId).stream()
                .map(this::mapProject)
                .toList();
    }

    @Override
    public void saveProject(TenantProject project) {
        mapper.upsertProject(project);
    }

    private TenantProject mapProject(Map<String, Object> row) {
        return new TenantProject(
                RowValueHelper.string(row, "id"),
                RowValueHelper.string(row, "tenantId", "tenant_id"),
                RowValueHelper.string(row, "name"),
                RowValueHelper.bool(row, "active")
        );
    }
}
