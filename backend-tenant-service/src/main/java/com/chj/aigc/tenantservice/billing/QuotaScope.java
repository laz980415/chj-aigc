package com.chj.aigc.tenantservice.billing;

/**
 * 额度作用范围。
 */
public record QuotaScope(
        QuotaScopeType scopeType,
        String scopeId
) {
}
