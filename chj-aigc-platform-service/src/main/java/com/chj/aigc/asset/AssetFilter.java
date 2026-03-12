package com.chj.aigc.asset;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public record AssetFilter(
        Optional<String> tenantId,
        Optional<String> projectId,
        Optional<String> clientId,
        Optional<String> brandId,
        Optional<AssetKind> kind,
        Set<String> requiredTags
) {
    public AssetFilter {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(projectId, "projectId");
        Objects.requireNonNull(clientId, "clientId");
        Objects.requireNonNull(brandId, "brandId");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(requiredTags, "requiredTags");
        requiredTags = Set.copyOf(requiredTags);
    }

    public static AssetFilter byBrand(String tenantId, String brandId) {
        return new AssetFilter(
                Optional.of(tenantId),
                Optional.empty(),
                Optional.empty(),
                Optional.of(brandId),
                Optional.empty(),
                Set.of()
        );
    }
}
