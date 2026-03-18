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
        payload.put("userTokenRemaining", remaining(tenantId, QuotaScopeType.USER, "user-tenant-owner", QuotaDimension.TOKENS));
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

    /**
     * 对已成功的生成任务执行额度扣减和钱包扣费。
     */
    public GenerationSettlement settleGeneration(
            String jobId,
            String tenantId,
            String projectId,
            String userId,
            String capability,
            Integer inputTokens,
            Integer outputTokens,
            Integer imageCount,
            Integer videoSeconds
    ) {
        WalletLedgerEntry existingEntry = store.listLedgerEntries(tenantId).stream()
                .filter(entry -> entry.type() == LedgerEntryType.CONSUMPTION)
                .filter(entry -> jobId.equals(entry.referenceId()))
                .findFirst()
                .orElse(null);
        int finalInputTokens = inputTokens != null ? inputTokens : 0;
        int finalOutputTokens = outputTokens != null ? outputTokens : 0;
        int finalImageCount = imageCount != null ? imageCount : 0;
        int finalVideoSeconds = videoSeconds != null ? videoSeconds : 0;
        if ("copywriting".equalsIgnoreCase(capability)) {
            finalInputTokens = finalInputTokens > 0 ? finalInputTokens : 48;
            finalOutputTokens = finalOutputTokens > 0 ? finalOutputTokens : 96;
        } else if ("image_generation".equalsIgnoreCase(capability)) {
            finalImageCount = finalImageCount > 0 ? finalImageCount : 1;
        } else if ("video_generation".equalsIgnoreCase(capability)) {
            finalVideoSeconds = finalVideoSeconds > 0 ? finalVideoSeconds : 10;
        }

        BigDecimal chargeAmount = calculateCharge(capability, finalInputTokens, finalOutputTokens, finalImageCount, finalVideoSeconds);
        if (existingEntry != null) {
            return new GenerationSettlement(
                    existingEntry.amount().amount(),
                    finalInputTokens,
                    finalOutputTokens,
                    finalImageCount,
                    finalVideoSeconds
            );
        }

        ensureQuotaAvailable(tenantId, new QuotaScope(QuotaScopeType.USER, userId), capability, finalInputTokens, finalOutputTokens, finalImageCount, finalVideoSeconds);
        ensureQuotaAvailable(tenantId, new QuotaScope(QuotaScopeType.PROJECT, projectId), capability, finalInputTokens, finalOutputTokens, finalImageCount, finalVideoSeconds);
        if (walletBalance(tenantId).compareTo(chargeAmount) < 0) {
            throw new IllegalStateException("钱包余额不足，无法完成本次生成结算");
        }

        consumeQuotaIfPresent(tenantId, new QuotaScope(QuotaScopeType.USER, userId), capability, finalInputTokens, finalOutputTokens, finalImageCount, finalVideoSeconds);
        consumeQuotaIfPresent(tenantId, new QuotaScope(QuotaScopeType.PROJECT, projectId), capability, finalInputTokens, finalOutputTokens, finalImageCount, finalVideoSeconds);
        store.saveLedgerEntry(new WalletLedgerEntry(
                "usage-" + jobId,
                tenantId,
                LedgerEntryType.CONSUMPTION,
                new Money(chargeAmount),
                "生成任务扣费：" + capability,
                jobId,
                Instant.now()
        ));
        return new GenerationSettlement(chargeAmount, finalInputTokens, finalOutputTokens, finalImageCount, finalVideoSeconds);
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

    private void ensureQuotaAvailable(
            String tenantId,
            QuotaScope scope,
            String capability,
            int inputTokens,
            int outputTokens,
            int imageCount,
            int videoSeconds
    ) {
        QuotaDimension dimension = quotaDimension(capability);
        BigDecimal required = requiredAmount(capability, inputTokens, outputTokens, imageCount, videoSeconds);
        QuotaAllocation allocation = findQuotaAllocation(tenantId, scope, dimension);
        if (allocation != null && allocation.limit().subtract(allocation.used()).compareTo(required) < 0) {
            throw new IllegalStateException("额度不足：" + scope.scopeType().name() + ":" + scope.scopeId() + ":" + dimension.name());
        }
    }

    private void consumeQuotaIfPresent(
            String tenantId,
            QuotaScope scope,
            String capability,
            int inputTokens,
            int outputTokens,
            int imageCount,
            int videoSeconds
    ) {
        QuotaDimension dimension = quotaDimension(capability);
        BigDecimal required = requiredAmount(capability, inputTokens, outputTokens, imageCount, videoSeconds);
        QuotaAllocation allocation = findQuotaAllocation(tenantId, scope, dimension);
        if (allocation == null) {
            return;
        }
        store.saveQuotaAllocation(new QuotaAllocation(
                allocation.id(),
                allocation.tenantId(),
                allocation.scope(),
                allocation.dimension(),
                allocation.limit(),
                allocation.used().add(required)
        ));
    }

    private QuotaAllocation findQuotaAllocation(String tenantId, QuotaScope scope, QuotaDimension dimension) {
        return store.listQuotaAllocations(tenantId).stream()
                .filter(item -> item.scope().scopeType() == scope.scopeType())
                .filter(item -> item.scope().scopeId().equals(scope.scopeId()))
                .filter(item -> item.dimension() == dimension)
                .findFirst()
                .orElse(null);
    }

    private QuotaDimension quotaDimension(String capability) {
        return switch (capability.toLowerCase()) {
            case "copywriting" -> QuotaDimension.TOKENS;
            case "image_generation" -> QuotaDimension.IMAGE_COUNT;
            case "video_generation" -> QuotaDimension.VIDEO_SECONDS;
            default -> throw new IllegalArgumentException("不支持的生成能力: " + capability);
        };
    }

    private BigDecimal requiredAmount(String capability, int inputTokens, int outputTokens, int imageCount, int videoSeconds) {
        return switch (capability.toLowerCase()) {
            case "copywriting" -> new BigDecimal(inputTokens + outputTokens);
            case "image_generation" -> new BigDecimal(imageCount);
            case "video_generation" -> new BigDecimal(videoSeconds);
            default -> throw new IllegalArgumentException("不支持的生成能力: " + capability);
        };
    }

    private BigDecimal calculateCharge(String capability, int inputTokens, int outputTokens, int imageCount, int videoSeconds) {
        BigDecimal charge = switch (capability.toLowerCase()) {
            case "copywriting" -> new BigDecimal(inputTokens + outputTokens).multiply(new BigDecimal("0.0001"));
            case "image_generation" -> new BigDecimal(imageCount).multiply(new BigDecimal("8.0000"));
            case "video_generation" -> new BigDecimal(videoSeconds).multiply(new BigDecimal("1.5000"));
            default -> throw new IllegalArgumentException("不支持的生成能力: " + capability);
        };
        return charge.setScale(4, java.math.RoundingMode.HALF_UP);
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
                "tenant-service-project-video-quota",
                tenantId,
                new QuotaScope(QuotaScopeType.PROJECT, "project-demo"),
                QuotaDimension.VIDEO_SECONDS,
                new BigDecimal("120"),
                BigDecimal.ZERO
        ));
        store.saveQuotaAllocation(new QuotaAllocation(
                "tenant-service-owner-token-quota",
                tenantId,
                new QuotaScope(QuotaScopeType.USER, "user-tenant-owner"),
                QuotaDimension.TOKENS,
                new BigDecimal("50000"),
                BigDecimal.ZERO
        ));
        store.saveQuotaAllocation(new QuotaAllocation(
                "tenant-service-member-token-quota",
                tenantId,
                new QuotaScope(QuotaScopeType.USER, "user-tenant-member"),
                QuotaDimension.TOKENS,
                new BigDecimal("50000"),
                BigDecimal.ZERO
        ));
    }

    public record GenerationSettlement(
            BigDecimal chargeAmount,
            Integer inputTokens,
            Integer outputTokens,
            Integer imageCount,
            Integer videoSeconds
    ) {
    }
}
