package com.chj.aigc.web.dto;

public record CreateUserRequest(
        String userId,
        String username,
        String password,
        String displayName,
        String roleKey,
        String tenantId
) {
}
