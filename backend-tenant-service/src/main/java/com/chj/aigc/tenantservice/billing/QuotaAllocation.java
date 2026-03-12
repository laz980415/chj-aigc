package com.chj.aigc.tenantservice.billing;

import java.math.BigDecimal;

/**
 * 租户额度分配记录。
 */
public record QuotaAllocation(
        String id,
        String tenantId,
        QuotaScope scope,
        QuotaDimension dimension,
        BigDecimal limit,
        BigDecimal used
) {
}
