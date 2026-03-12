package com.chj.aigc.billing;

import java.util.Objects;

public record QuotaScope(
        QuotaScopeType scopeType,
        String scopeId
) {
    public QuotaScope {
        Objects.requireNonNull(scopeType, "scopeType");
        Objects.requireNonNull(scopeId, "scopeId");
    }
}
