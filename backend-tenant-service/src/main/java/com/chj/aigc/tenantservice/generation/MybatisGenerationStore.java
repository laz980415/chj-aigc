package com.chj.aigc.tenantservice.generation;

import com.chj.aigc.tenantservice.persistence.RowValueHelper;
import com.chj.aigc.tenantservice.persistence.mapper.GenerationMapper;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 基于 MyBatis 的生成任务存储。
 */
public final class MybatisGenerationStore implements GenerationStore {
    private final GenerationMapper mapper;

    public MybatisGenerationStore(GenerationMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<GenerationJob> listJobs(String tenantId) {
        return mapper.listJobs(tenantId).stream().map(this::mapJob).toList();
    }

    @Override
    public Optional<GenerationJob> findJob(String jobId) {
        return Optional.ofNullable(mapper.findJob(jobId)).map(this::mapJob);
    }

    @Override
    public void saveJob(GenerationJob job) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", job.id());
        payload.put("tenantId", job.tenantId());
        payload.put("projectId", job.projectId());
        payload.put("actorId", job.actorId());
        payload.put("roleKey", job.roleKey());
        payload.put("modelAlias", job.modelAlias());
        payload.put("capability", job.capability().value());
        payload.put("brandId", job.brandId());
        payload.put("brandName", job.brandName());
        payload.put("brandSummary", job.brandSummary());
        payload.put("clientName", job.clientName());
        payload.put("userPrompt", job.userPrompt());
        payload.put("status", job.status().value());
        payload.put("outputText", job.outputText());
        payload.put("outputUri", job.outputUri());
        payload.put("errorMessage", job.errorMessage());
        payload.put("providerId", job.providerId());
        payload.put("providerModelName", job.providerModelName());
        payload.put("providerJobId", job.providerJobId());
        payload.put("inputTokens", job.inputTokens());
        payload.put("outputTokens", job.outputTokens());
        payload.put("imageCount", job.imageCount());
        payload.put("videoSeconds", job.videoSeconds());
        payload.put("chargeAmount", job.chargeAmount());
        payload.put("settled", job.settled());
        payload.put("createdAt", Timestamp.from(job.createdAt()));
        payload.put("updatedAt", Timestamp.from(job.updatedAt()));
        mapper.upsertJob(payload);
    }

    private GenerationJob mapJob(Map<String, Object> row) {
        return new GenerationJob(
                RowValueHelper.string(row, "id"),
                RowValueHelper.string(row, "tenantId", "tenant_id"),
                RowValueHelper.string(row, "projectId", "project_id"),
                RowValueHelper.string(row, "actorId", "actor_id"),
                RowValueHelper.string(row, "roleKey", "role_key"),
                RowValueHelper.string(row, "modelAlias", "model_alias"),
                GenerationCapability.fromValue(RowValueHelper.string(row, "capability")),
                RowValueHelper.string(row, "brandId", "brand_id"),
                defaultString(RowValueHelper.string(row, "brandName", "brand_name")),
                defaultString(RowValueHelper.string(row, "brandSummary", "brand_summary")),
                defaultString(RowValueHelper.string(row, "clientName", "client_name")),
                defaultString(RowValueHelper.string(row, "userPrompt", "user_prompt")),
                GenerationJobStatus.fromValue(RowValueHelper.string(row, "status")),
                defaultString(RowValueHelper.string(row, "outputText", "output_text")),
                defaultString(RowValueHelper.string(row, "outputUri", "output_uri")),
                defaultString(RowValueHelper.string(row, "errorMessage", "error_message")),
                defaultString(RowValueHelper.string(row, "providerId", "provider_id")),
                defaultString(RowValueHelper.string(row, "providerModelName", "provider_model_name")),
                defaultString(RowValueHelper.string(row, "providerJobId", "provider_job_id")),
                integerValue(row, "inputTokens", "input_tokens"),
                integerValue(row, "outputTokens", "output_tokens"),
                integerValue(row, "imageCount", "image_count"),
                integerValue(row, "videoSeconds", "video_seconds"),
                RowValueHelper.decimal(row, "chargeAmount", "charge_amount"),
                RowValueHelper.bool(row, "settled"),
                RowValueHelper.timestamp(row, "createdAt", "created_at").toInstant(),
                RowValueHelper.timestamp(row, "updatedAt", "updated_at").toInstant()
        );
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    private Integer integerValue(Map<String, Object> row, String... keys) {
        String value = RowValueHelper.string(row, keys);
        return value == null || value.isBlank() ? null : Integer.valueOf(value);
    }
}
