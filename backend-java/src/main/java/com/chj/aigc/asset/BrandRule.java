package com.chj.aigc.asset;

import java.util.Objects;

public record BrandRule(
        String id,
        String tenantId,
        String brandId,
        BrandRuleKind kind,
        String content,
        boolean active
) {
    public BrandRule {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(brandId, "brandId");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(content, "content");
    }
}
