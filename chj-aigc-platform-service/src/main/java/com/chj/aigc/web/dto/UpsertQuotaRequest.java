package com.chj.aigc.web.dto;

public record UpsertQuotaRequest(
        String allocationId,
        String scopeType,
        String scopeId,
        String dimension,
        String limit,
        String used
) {
}
