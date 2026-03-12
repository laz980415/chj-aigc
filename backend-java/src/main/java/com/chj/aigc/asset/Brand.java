package com.chj.aigc.asset;

import java.util.Objects;

public record Brand(
        String id,
        String tenantId,
        String clientId,
        String name,
        String summary,
        boolean active
) {
    public Brand {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(clientId, "clientId");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(summary, "summary");
    }
}
