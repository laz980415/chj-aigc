package com.chj.aigc.billing;

import java.time.Instant;
import java.util.Objects;

/**
 * 充值支付订单。
 * 用于承载微信支付下单信息和模拟支付状态，支付完成后再落钱包流水。
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
