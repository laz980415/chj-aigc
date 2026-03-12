package com.chj.aigc.tenantservice.asset;

import java.util.Objects;
import java.util.Set;

/**
 * 租户素材实体。
 */
public record Asset(
        String id,
        String tenantId,
        String projectId,
        String clientId,
        String brandId,
        String name,
        AssetKind kind,
        String uri,
        Set<String> tags,
        boolean active
) {
    public Asset {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(projectId, "projectId");
        Objects.requireNonNull(clientId, "clientId");
        Objects.requireNonNull(brandId, "brandId");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(uri, "uri");
        Objects.requireNonNull(tags, "tags");
        tags = Set.copyOf(tags);
    }
}
