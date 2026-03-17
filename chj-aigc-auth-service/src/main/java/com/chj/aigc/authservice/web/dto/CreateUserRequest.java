package com.chj.aigc.authservice.web.dto;

/**
 * 创建账号请求体。
 */
public record CreateUserRequest(
        String userId,
        String username,
        String password,
        String displayName,
        String roleKey,
        String tenantId
) {}
