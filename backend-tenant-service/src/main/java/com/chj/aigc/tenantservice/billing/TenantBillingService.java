package com.chj.aigc.tenantservice.billing;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 租户额度编排服务。
 */
public final class TenantBillingService {
    private final TenantBillingStore store;

    public TenantBillingService(TenantBillingStore store) {
        this.store = store;
        seedIfNeeded("tenant-demo");
    }

    /**
     * 返回租户钱包摘要。
     */
    public Map<String, Object> walletSnapshot(String tenantId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tenantId", tenantId);
        payload.put("balance", walletBalance(tenantId).toPlainString());
        payload.put("ledgerCount", store.listLedgerEntries(tenantId).size());
        return payload;
    }

    /**
     * 返回租户额度摘要。
     */
    public Map<String, Object> quotaSnapshot(String tenantId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("projectImageRemaining", remaining(tenantId, QuotaScopeType.PROJECT, "project-demo", QuotaDimension.IMAGE_COUNT));
        payload.put("userTokenRemaining", remaining(tenantId, QuotaScopeType.USER, "user-demo", QuotaDimension.TOKENS));
        return payload;
    }

    /**
     * 返回额度分配列表。
     */
    public List<QuotaAllocation> listQuotaAllocations(String tenantId) {
        return store.listQuotaAllocations(tenantId);
    }

    /**
     * 创建模拟微信支付订单。
     */
    public Map<String, Object> createMockWeChatPaymentOrder(
            String tenantId,
            String orderId,
            Money amount,
            String description,
            String referenceId
    ) {
        PaymentOrder order = new PaymentOrder(
                orderId,
                tenantId,
                PaymentChannel.WECHAT_NATIVE,
                PaymentOrderStatus.PENDING,
                amount,
                description,
                referenceId,
                "weixin://mock-pay/" + orderId,
                Instant.now(),
                null
        );
        store.savePaymentOrder(order);
        return serializePaymentOrder(order);
    }

    /**
     * 将模拟支付订单置为已支付，并写入钱包充值流水。
     */
    public Map<String, Object> markPaymentOrderPaid(String orderId) {
        PaymentOrder existing = store.findPaymentOrder(orderId);
        if (existing == null) {
            throw new IllegalArgumentException("支付订单不存在");
        }
        if (existing.status() == PaymentOrderStatus.PAID) {
            return serializePaymentOrder(existing);
        }

        PaymentOrder paidOrder = new PaymentOrder(
                existing.id(),
                existing.tenantId(),
                existing.channel(),
                PaymentOrderStatus.PAID,
                existing.amount(),
                existing.description(),
                existing.referenceId(),
                existing.qrCode(),
                existing.createdAt(),
                Instant.now()
        );
        store.savePaymentOrder(paidOrder);
        store.saveLedgerEntry(new WalletLedgerEntry(
                "ledger-" + paidOrder.id(),
                paidOrder.tenantId(),
                LedgerEntryType.RECHARGE,
                paidOrder.amount(),
                "微信支付到账：" + paidOrder.description(),
                paidOrder.referenceId(),
                Instant.now()
        ));
        return serializePaymentOrder(paidOrder);
    }

    /**
     * 返回租户支付订单。
     */
    public List<Map<String, Object>> paymentOrders(String tenantId) {
        return store.listPaymentOrders(tenantId).stream()
                .map(this::serializePaymentOrder)
                .toList();
    }

    /**
     * 保存额度分配。
     */
    public Map<String, Object> upsertQuota(QuotaAllocation allocation) {
        store.saveQuotaAllocation(allocation);
        return quotaSnapshot(allocation.tenantId());
    }

    private BigDecimal remaining(String tenantId, QuotaScopeType scopeType, String scopeId, QuotaDimension dimension) {
        return store.listQuotaAllocations(tenantId).stream()
                .filter(item -> item.scope().scopeType() == scopeType)
                .filter(item -> item.scope().scopeId().equals(scopeId))
                .filter(item -> item.dimension() == dimension)
                .findFirst()
                .map(item -> item.limit().subtract(item.used()))
                .orElse(BigDecimal.ZERO);
    }

    private BigDecimal walletBalance(String tenantId) {
        return store.listLedgerEntries(tenantId).stream()
                .map(entry -> switch (entry.type()) {
                    case RECHARGE -> entry.amount().amount();
                    case CONSUMPTION -> entry.amount().amount().negate();
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Map<String, Object> serializePaymentOrder(PaymentOrder order) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", order.id());
        payload.put("tenantId", order.tenantId());
        payload.put("channel", order.channel().name());
        payload.put("status", order.status().name());
        payload.put("amount", order.amount().amount().toPlainString());
        payload.put("description", order.description());
        payload.put("referenceId", order.referenceId());
        payload.put("qrCode", order.qrCode());
        payload.put("createdAt", order.createdAt());
        payload.put("paidAt", order.paidAt());
        return payload;
    }

    private void seedIfNeeded(String tenantId) {
        if (store.listLedgerEntries(tenantId).isEmpty()) {
            store.saveLedgerEntry(new WalletLedgerEntry(
                    "recharge-seed-1",
                    tenantId,
                    LedgerEntryType.RECHARGE,
                    Money.of("1000"),
                    "seed balance",
                    "seed",
                    Instant.now()
            ));
        }
        if (!store.listQuotaAllocations(tenantId).isEmpty()) {
            return;
        }
        store.saveQuotaAllocation(new QuotaAllocation(
                "tenant-service-project-quota",
                tenantId,
                new QuotaScope(QuotaScopeType.PROJECT, "project-demo"),
                QuotaDimension.IMAGE_COUNT,
                new BigDecimal("20"),
                BigDecimal.ZERO
        ));
        store.saveQuotaAllocation(new QuotaAllocation(
                "tenant-service-user-quota",
                tenantId,
                new QuotaScope(QuotaScopeType.USER, "user-demo"),
                QuotaDimension.TOKENS,
                new BigDecimal("50000"),
                new BigDecimal("1200")
        ));
    }
}
