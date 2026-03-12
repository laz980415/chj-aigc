package com.chj.aigc.tenant;

import com.chj.aigc.auth.AuthService;
import com.chj.aigc.auth.AuthUser;
import java.util.List;

public final class TenantWorkspaceService {
    private final TenantProjectStore projectStore;
    private final AuthService authService;

    public TenantWorkspaceService(TenantProjectStore projectStore, AuthService authService) {
        this.projectStore = projectStore;
        this.authService = authService;
        seedIfNeeded("tenant-demo");
    }

    public List<TenantProject> projects(String tenantId) {
        return projectStore.listProjects(tenantId).stream()
                .filter(TenantProject::active)
                .toList();
    }

    public TenantProject createProject(String projectId, String tenantId, String name) {
        TenantProject project = new TenantProject(projectId, tenantId, name, true);
        projectStore.saveProject(project);
        return project;
    }

    public List<AuthUser> members(String tenantId) {
        return authService.listUsers().stream()
                .filter(AuthUser::active)
                .filter(user -> tenantId.equals(user.tenantId()))
                .toList();
    }

    private void seedIfNeeded(String tenantId) {
        if (projectStore.listProjects(tenantId).isEmpty()) {
            projectStore.saveProject(new TenantProject("project-demo", tenantId, "Demo Campaign", true));
        }
    }
}
