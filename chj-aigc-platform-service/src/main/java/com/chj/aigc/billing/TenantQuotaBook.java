package com.chj.aigc.billing;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class TenantQuotaBook {
    private final String tenantId;
    private final Map<String, QuotaAllocation> allocations = new HashMap<>();

    public TenantQuotaBook(String tenantId) {
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
    }

    public void upsert(QuotaAllocation allocation) {
        if (!tenantId.equals(allocation.tenantId())) {
            throw new IllegalArgumentException("Quota allocation tenant mismatch");
        }
        allocations.put(key(allocation.scope(), allocation.dimension()), allocation);
    }

    public Optional<QuotaAllocation> find(QuotaScope scope, QuotaDimension dimension) {
        return Optional.ofNullable(allocations.get(key(scope, dimension)));
    }

    public boolean canConsume(QuotaRequest request) {
        return find(request.scope(), request.dimension())
                .map(allocation -> allocation.remaining().compareTo(request.quantity()) >= 0)
                .orElse(false);
    }

    public QuotaAllocation consume(QuotaRequest request) {
        QuotaAllocation existing = find(request.scope(), request.dimension())
                .orElseThrow(() -> new IllegalStateException("quota allocation not found"));
        QuotaAllocation updated = existing.consume(request.quantity());
        upsert(updated);
        return updated;
    }

    public BigDecimal remaining(QuotaScope scope, QuotaDimension dimension) {
        return find(scope, dimension)
                .map(QuotaAllocation::remaining)
                .orElse(BigDecimal.ZERO);
    }

    private String key(QuotaScope scope, QuotaDimension dimension) {
        return scope.scopeType().name() + ":" + scope.scopeId() + ":" + dimension.name();
    }
}
