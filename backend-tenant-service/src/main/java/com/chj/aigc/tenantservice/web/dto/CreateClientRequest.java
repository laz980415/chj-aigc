package com.chj.aigc.tenantservice.web.dto;

/**
 * 创建租户客户请求。
 */
public record CreateClientRequest(
        String clientId,
        String name
) {
}
