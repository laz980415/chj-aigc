package com.chj.aigc.tenantservice.asset;

import java.util.Objects;

/**
 * 广告主客户实体。
 */
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
