package com.chj.aigc.tenantservice.tenant;

import java.util.List;

/**
 * 租户项目存储接口。
 */
public interface TenantProjectStore {
    List<TenantProject> listProjects(String tenantId);

    void saveProject(TenantProject project);
}
