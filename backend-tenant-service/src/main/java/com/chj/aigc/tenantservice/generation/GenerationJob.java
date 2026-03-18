package com.chj.aigc.tenantservice.generation;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * 租户侧生成任务持久化实体。
 */
public record GenerationJob(
        String id,
        String tenantId,
        String projectId,
        String actorId,
        String roleKey,
        String modelAlias,
        GenerationCapability capability,
        String brandId,
        String brandName,
        String brandSummary,
        String clientName,
        String userPrompt,
        GenerationJobStatus status,
        String outputText,
        String outputUri,
        String errorMessage,
        String providerId,
        String providerModelName,
        String providerJobId,
        Integer inputTokens,
        Integer outputTokens,
        Integer imageCount,
        Integer videoSeconds,
        BigDecimal chargeAmount,
        boolean settled,
        Instant createdAt,
        Instant updatedAt
) {
    public GenerationJob {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(projectId, "projectId");
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(roleKey, "roleKey");
        Objects.requireNonNull(modelAlias, "modelAlias");
        Objects.requireNonNull(capability, "capability");
        Objects.requireNonNull(brandName, "brandName");
        Objects.requireNonNull(brandSummary, "brandSummary");
        Objects.requireNonNull(clientName, "clientName");
        Objects.requireNonNull(userPrompt, "userPrompt");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(outputText, "outputText");
        Objects.requireNonNull(outputUri, "outputUri");
        Objects.requireNonNull(errorMessage, "errorMessage");
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(providerModelName, "providerModelName");
        Objects.requireNonNull(providerJobId, "providerJobId");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
    }
}
