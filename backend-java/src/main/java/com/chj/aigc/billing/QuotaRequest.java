package com.chj.aigc.billing;

import java.math.BigDecimal;
import java.util.Objects;

public record QuotaRequest(
        QuotaScope scope,
        QuotaDimension dimension,
        BigDecimal quantity
) {
    public QuotaRequest {
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(dimension, "dimension");
        Objects.requireNonNull(quantity, "quantity");
        if (quantity.signum() < 0) {
            throw new IllegalArgumentException("quantity must be non-negative");
        }
    }
}
