package com.chj.aigc.tenantservice.web.dto;

/**
 * 登录请求体。
 */
public record LoginRequest(
        String username,
        String password
) {
}
