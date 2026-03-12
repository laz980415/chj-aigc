package com.chj.aigc.web.dto;

public record CreateModelAccessRuleRequest(
        String ruleId,
        String actorId,
        String platformModelAlias,
        String scopeType,
        String scopeValue,
        String effect,
        String reason
) {
}
