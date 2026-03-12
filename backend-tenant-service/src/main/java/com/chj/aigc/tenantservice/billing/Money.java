package com.chj.aigc.tenantservice.billing;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * 金额值对象。
 */
public record Money(BigDecimal amount) {
    public Money {
        Objects.requireNonNull(amount, "amount");
    }

    public static Money of(String amount) {
        return new Money(new BigDecimal(amount));
    }
}
