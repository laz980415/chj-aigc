package com.chj.aigc.tenant;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 基于 PostgreSQL 的租户项目存储实现。
 */
public final class JdbcTenantProjectStore implements TenantProjectStore {
    private final JdbcTemplate jdbcTemplate;

    public JdbcTenantProjectStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<TenantProject> listProjects(String tenantId) {
        return jdbcTemplate.query(
                """
                select id, tenant_id, name, active
                from tenant_projects
                where tenant_id = ?
                order by name asc
                """,
                (resultSet, rowNum) -> mapProject(resultSet),
                tenantId
        );
    }

    @Override
    public void saveProject(TenantProject project) {
        jdbcTemplate.update(
                """
                insert into tenant_projects (id, tenant_id, name, active)
                values (?, ?, ?, ?)
                on conflict (id) do update set
                    tenant_id = excluded.tenant_id,
                    name = excluded.name,
                    active = excluded.active
                """,
                project.id(),
                project.tenantId(),
                project.name(),
                project.active()
        );
    }

    private TenantProject mapProject(ResultSet resultSet) throws SQLException {
        return new TenantProject(
                resultSet.getString("id"),
                resultSet.getString("tenant_id"),
                resultSet.getString("name"),
                resultSet.getBoolean("active")
        );
    }
}
