package com.chj.aigc.tenantservice.billing;

import java.util.List;

/**
 * 租户额度存储接口。
 */
public interface TenantBillingStore {
    List<QuotaAllocation> listQuotaAllocations(String tenantId);

    void saveQuotaAllocation(QuotaAllocation allocation);
}
