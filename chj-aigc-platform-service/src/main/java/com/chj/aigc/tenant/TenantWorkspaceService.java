package com.chj.aigc.tenant;

import com.chj.aigc.auth.AuthService;
import com.chj.aigc.auth.AuthUser;
import java.util.List;

/**
 * 负责租户内部主数据编排。
 * 当前聚焦项目列表和租户成员列表，供租户工作台读取和创建操作复用。
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
     * 返回租户下所有启用中的项目。
     */
    public List<TenantProject> projects(String tenantId) {
        return projectStore.listProjects(tenantId).stream()
                .filter(TenantProject::active)
                .toList();
    }

    /**
     * 创建新的租户项目。
     */
    public TenantProject createProject(String projectId, String tenantId, String name) {
        TenantProject project = new TenantProject(projectId, tenantId, name, true);
        projectStore.saveProject(project);
        return project;
    }

    /**
     * 返回租户下所有有效成员，用于成员列表和额度配置下拉。
     */
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
