package com.chj.aigc.web.dto;

public record RechargeRequest(
        String entryId,
        String amount,
        String description,
        String referenceId
) {
}
