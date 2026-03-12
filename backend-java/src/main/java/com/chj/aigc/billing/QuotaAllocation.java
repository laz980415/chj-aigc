package com.chj.aigc.billing;

import java.math.BigDecimal;
import java.util.Objects;

public record QuotaAllocation(
        String id,
        String tenantId,
        QuotaScope scope,
        QuotaDimension dimension,
        BigDecimal limit,
        BigDecimal used
) {
    public QuotaAllocation {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(dimension, "dimension");
        Objects.requireNonNull(limit, "limit");
        Objects.requireNonNull(used, "used");
        if (limit.signum() < 0) {
            throw new IllegalArgumentException("limit must be non-negative");
        }
        if (used.signum() < 0) {
            throw new IllegalArgumentException("used must be non-negative");
        }
    }

    public BigDecimal remaining() {
        return limit.subtract(used);
    }

    public QuotaAllocation consume(BigDecimal quantity) {
        Objects.requireNonNull(quantity, "quantity");
        if (quantity.signum() < 0) {
            throw new IllegalArgumentException("quantity must be non-negative");
        }
        BigDecimal updated = used.add(quantity);
        if (updated.compareTo(limit) > 0) {
            throw new IllegalStateException("quota exceeded");
        }
        return new QuotaAllocation(id, tenantId, scope, dimension, limit, updated);
    }
}
