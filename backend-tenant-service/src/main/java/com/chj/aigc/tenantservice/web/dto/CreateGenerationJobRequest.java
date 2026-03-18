package com.chj.aigc.tenantservice.web.dto;

/**
 * 提交生成任务请求。
 */
public record CreateGenerationJobRequest(
        String projectId,
        String modelAlias,
        String capability,
        String userPrompt,
        String brandId,
        String brandName,
        String brandSummary
) {
}
