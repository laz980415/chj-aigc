package com.chj.aigc.tenantservice.generation;

import java.util.List;

/**
 * Python 模型网关客户端。
 */
public interface ModelGatewayClient {
    JobResult submitJob(SubmitPayload payload);

    JobResult fetchJob(String jobId);

    record SubmitPayload(
            String tenantId,
            String projectId,
            String actorId,
            String modelAlias,
            String capability,
            String userPrompt,
            String clientName,
            String brandName,
            String brandSummary,
            List<AssetPayload> assets
    ) {
    }

    record AssetPayload(
            String id,
            String name,
            String kind,
            String uri,
            List<String> tags,
            String summary
    ) {
    }

    record JobResult(
            String jobId,
            String status,
            String outputText,
            String outputUri,
            String errorMessage,
            String providerId,
            String providerModelName,
            String providerJobId,
            Integer inputTokens,
            Integer outputTokens,
            Integer imageCount,
            Integer videoSeconds
    ) {
    }
}
