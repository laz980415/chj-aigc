package com.chj.aigc.tenantservice.billing;

import java.math.BigDecimal;
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

    private void seedIfNeeded(String tenantId) {
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
