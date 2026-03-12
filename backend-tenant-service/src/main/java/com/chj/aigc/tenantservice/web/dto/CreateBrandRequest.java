package com.chj.aigc.tenantservice.web.dto;

/**
 * 创建租户品牌请求。
 */
public record CreateBrandRequest(
        String brandId,
        String clientId,
        String name,
        String summary
) {
}
