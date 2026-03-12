package com.chj.aigc.tenantservice.web.dto;

/**
 * 保存额度配置请求体。
 */
public record UpsertQuotaRequest(
        String allocationId,
        String scopeType,
        String scopeId,
        String dimension,
        String limit,
        String used
) {
}
