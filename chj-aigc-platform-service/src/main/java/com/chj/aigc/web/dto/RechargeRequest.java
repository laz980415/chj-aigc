package com.chj.aigc.web.dto;

public record RechargeRequest(
        String entryId,
        String tenantId,
        String amount,
        String description,
        String referenceId
) {
}
