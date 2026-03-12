package com.chj.aigc.tenantservice.tenant;

import com.chj.aigc.tenantservice.auth.AuthService;
import com.chj.aigc.tenantservice.auth.AuthUser;
import java.util.List;

/**
 * 租户工作台项目与成员编排。
 */
public final class TenantWorkspaceService {
    private final TenantProjectStore projectStore;
    private final AuthService authService;

    public TenantWorkspaceService(TenantProjectStore projectStore, AuthService authService) {
        this.projectStore = projectStore;
        this.authService = authService;
        seedIfNeeded("tenant-demo");
    }

    /**
     * 返回租户项目列表。
     */
    public List<TenantProject> projects(String tenantId) {
        return projectStore.listProjects(tenantId).stream()
                .filter(TenantProject::active)
                .toList();
    }

    /**
     * 创建租户项目。
     */
    public TenantProject createProject(String projectId, String tenantId, String name) {
        TenantProject project = new TenantProject(projectId, tenantId, name, true);
        projectStore.saveProject(project);
        return project;
    }

    /**
     * 返回租户成员列表。
     */
    public List<AuthUser> members(String tenantId) {
        return authService.listTenantUsers(tenantId);
    }

    private void seedIfNeeded(String tenantId) {
        if (projectStore.listProjects(tenantId).isEmpty()) {
            projectStore.saveProject(new TenantProject("project-demo", tenantId, "Demo Campaign", true));
        }
    }
}
