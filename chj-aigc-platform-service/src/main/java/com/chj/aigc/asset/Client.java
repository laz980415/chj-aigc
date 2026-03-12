package com.chj.aigc.asset;

import java.util.Objects;

public record Client(
        String id,
        String tenantId,
        String name,
        boolean active
) {
    public Client {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(name, "name");
    }
}
