package com.chj.aigc.billing;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 负责租户钱包和额度的读写编排。
 * 当前提供钱包快照、充值以及项目/成员额度配置能力。
 */
public final class TenantBillingService {
    private final TenantBillingStore store;
    private final TenantFinanceService financeService = new TenantFinanceService();

    public TenantBillingService(TenantBillingStore store) {
        this.store = store;
        seedIfNeeded("tenant-demo");
    }

    /**
     * 返回租户钱包摘要。
     */
    public Map<String, Object> walletSnapshot(String tenantId) {
        TenantWallet wallet = wallet(tenantId);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tenantId", tenantId);
        payload.put("balance", wallet.balance().amount().toPlainString());
        payload.put("ledgerCount", wallet.ledgerEntries().size());
        return payload;
    }

    public Map<String, Object> recharge(String tenantId, String entryId, Money amount, String description, String referenceId) {
        TenantWallet wallet = wallet(tenantId);
        WalletLedgerEntry entry = financeService.recharge(wallet, entryId, amount, description, referenceId);
        store.saveLedgerEntry(entry);
        return walletSnapshot(tenantId);
    }

    public Map<String, Object> quotaSnapshot(String tenantId) {
        TenantQuotaBook quotaBook = quotaBook(tenantId);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("projectImageRemaining", quotaBook.remaining(new QuotaScope(QuotaScopeType.PROJECT, "project-demo"), QuotaDimension.IMAGE_COUNT));
        payload.put("userTokenRemaining", quotaBook.remaining(new QuotaScope(QuotaScopeType.USER, "user-demo"), QuotaDimension.TOKENS));
        return payload;
    }

    public Map<String, Object> upsertQuota(QuotaAllocation allocation) {
        store.saveQuotaAllocation(allocation);
        return quotaSnapshot(allocation.tenantId());
    }

    /**
     * 创建模拟微信支付订单。
     * 订单创建后不会立即到账，需再走一次“模拟支付成功”把订单转为已支付并写入钱包流水。
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
     * 将模拟支付订单标记为已支付，并把充值金额写入租户钱包。
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
        recharge(
                paidOrder.tenantId(),
                "ledger-" + paidOrder.id(),
                paidOrder.amount(),
                "微信支付到账：" + paidOrder.description(),
                paidOrder.referenceId()
        );
        return serializePaymentOrder(paidOrder);
    }

    /**
     * 返回租户下的支付订单列表。
     */
    public List<Map<String, Object>> paymentOrders(String tenantId) {
        return store.listPaymentOrders(tenantId).stream()
                .map(this::serializePaymentOrder)
                .toList();
    }

    /**
     * 返回租户钱包流水明细，供平台超管查看充值与扣费记录。
     */
    public List<Map<String, Object>> ledgerEntries(String tenantId) {
        return store.listLedgerEntries(tenantId).stream()
                .map(entry -> {
                    Map<String, Object> payload = new LinkedHashMap<>();
                    payload.put("id", entry.id());
                    payload.put("tenantId", entry.tenantId());
                    payload.put("entryType", entry.type().name());
                    payload.put("amount", entry.amount().amount().toPlainString());
                    payload.put("description", entry.description());
                    payload.put("referenceId", entry.referenceId());
                    payload.put("createdAt", entry.createdAt());
                    return payload;
                })
                .toList();
    }

    /**
     * 返回租户下全部额度分配，供页面展示项目额度和成员额度明细。
     */
    public List<QuotaAllocation> listQuotaAllocations(String tenantId) {
        return store.listQuotaAllocations(tenantId);
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

    private TenantWallet wallet(String tenantId) {
        TenantWallet wallet = new TenantWallet(tenantId);
        store.listLedgerEntries(tenantId).forEach(wallet::append);
        return wallet;
    }

    private TenantQuotaBook quotaBook(String tenantId) {
        TenantQuotaBook quotaBook = new TenantQuotaBook(tenantId);
        store.listQuotaAllocations(tenantId).forEach(quotaBook::upsert);
        return quotaBook;
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
                    java.time.Instant.now()
            ));
        }

        if (store.listQuotaAllocations(tenantId).isEmpty()) {
            store.saveQuotaAllocation(new QuotaAllocation(
                    "quota-project-1",
                    tenantId,
                    new QuotaScope(QuotaScopeType.PROJECT, "project-demo"),
                    QuotaDimension.IMAGE_COUNT,
                    new BigDecimal("20"),
                    BigDecimal.ZERO
            ));
            store.saveQuotaAllocation(new QuotaAllocation(
                    "quota-user-1",
                    tenantId,
                    new QuotaScope(QuotaScopeType.USER, "user-demo"),
                    QuotaDimension.TOKENS,
                    new BigDecimal("50000"),
                    new BigDecimal("1200")
            ));
        }
    }
}
