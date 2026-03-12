package com.chj.aigc.access;

import java.util.Objects;
import java.util.Set;

public record ModelAccessRequest(
        String tenantId,
        String projectId,
        Set<String> roleKeys,
        String platformModelAlias
) {
    public ModelAccessRequest {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(projectId, "projectId");
        Objects.requireNonNull(roleKeys, "roleKeys");
        Objects.requireNonNull(platformModelAlias, "platformModelAlias");
    }
}
