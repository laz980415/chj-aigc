package com.chj.aigc.tenantservice.billing;

import java.time.Instant;
import java.util.Objects;

/**
 * 支付订单实体。
 */
public record PaymentOrder(
        String id,
        String tenantId,
        PaymentChannel channel,
        PaymentOrderStatus status,
        Money amount,
        String description,
        String referenceId,
        String qrCode,
        Instant createdAt,
        Instant paidAt
) {
    public PaymentOrder {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(channel, "channel");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(referenceId, "referenceId");
        Objects.requireNonNull(qrCode, "qrCode");
        Objects.requireNonNull(createdAt, "createdAt");
    }
}
