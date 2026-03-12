package com.chj.aigc.tenantservice.auth;

/**
 * 远程认证接口的统一返回结构映射。
 */
public record ApiResponse<T>(int code, String message, T data) {
}
