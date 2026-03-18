package com.chj.aigc.provider;

import java.time.Instant;

/**
 * 模型供应商配置。
 * 超管在平台后台维护，模型网关启动时拉取。
 */
public record ProviderConfig(
        String id,
        String providerId,
        String displayName,
        String apiBaseUrl,
        String apiKey,          // 生产环境应加密，当前明文存储
        boolean enabled,
        String updatedBy,
        Instant updatedAt
) {}
