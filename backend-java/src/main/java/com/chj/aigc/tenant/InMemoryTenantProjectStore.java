package com.chj.aigc.tenant;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class InMemoryTenantProjectStore implements TenantProjectStore {
    private final List<TenantProject> projects = new ArrayList<>();

    @Override
    public List<TenantProject> listProjects(String tenantId) {
        return projects.stream()
                .filter(project -> project.tenantId().equals(tenantId))
                .toList();
    }

    @Override
    public void saveProject(TenantProject project) {
        TenantProject value = Objects.requireNonNull(project, "project");
        projects.removeIf(existing -> existing.id().equals(value.id()));
        projects.add(value);
    }
}
