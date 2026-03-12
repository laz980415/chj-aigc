package com.chj.aigc.persistence.mapper;

import com.chj.aigc.tenant.TenantProject;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

/**
 * 租户项目主数据的 MyBatis 映射接口。
 */
public interface TenantProjectMapper {
    List<Map<String, Object>> listProjects(@Param("tenantId") String tenantId);

    void upsertProject(TenantProject project);
}
