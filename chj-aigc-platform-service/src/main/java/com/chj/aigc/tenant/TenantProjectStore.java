package com.chj.aigc.tenant;

import java.util.List;

public interface TenantProjectStore {
    List<TenantProject> listProjects(String tenantId);

    void saveProject(TenantProject project);
}
