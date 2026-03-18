package com.chj.aigc.web.dto;

public record UpsertProviderConfigRequest(
        String providerId,
        String displayName,
        String apiBaseUrl,
        String apiKey,
        boolean enabled,
        String updatedBy
) {}
