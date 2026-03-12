package com.chj.aigc.tenantservice.persistence.mapper;

import com.chj.aigc.tenantservice.tenant.TenantProject;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

/**
 * 租户项目 MyBatis 映射接口。
 */
public interface TenantProjectMapper {
    List<Map<String, Object>> listProjects(@Param("tenantId") String tenantId);

    void upsertProject(TenantProject project);
}
