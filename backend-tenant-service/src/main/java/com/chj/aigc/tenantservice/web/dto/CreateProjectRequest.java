package com.chj.aigc.tenantservice.web.dto;

/**
 * 创建项目请求体。
 */
public record CreateProjectRequest(
        String projectId,
        String name
) {
}
