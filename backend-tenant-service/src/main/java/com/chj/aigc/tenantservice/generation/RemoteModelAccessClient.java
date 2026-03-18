package com.chj.aigc.tenantservice.generation;

import java.util.Map;
import java.util.Set;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/**
 * 通过平台服务内部接口做模型权限校验。
 */
public final class RemoteModelAccessClient implements ModelAccessClient {
    private final RestClient restClient;

    public RemoteModelAccessClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ModelAccessDecision evaluate(String tenantId, String projectId, Set<String> roleKeys, String modelAlias) {
        Map<String, Object> envelope = restClient.post()
                .uri("/internal/model-access/decision")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(new EvaluateRequest(
                        tenantId,
                        projectId,
                        roleKeys,
                        modelAlias
                ))
                .retrieve()
                .body(Map.class);
        Map<String, Object> payload = envelope != null && envelope.get("data") instanceof Map<?, ?> data
                ? (Map<String, Object>) data
                : Map.of();
        return new ModelAccessDecision(
                Boolean.TRUE.equals(payload.get("allowed")),
                String.valueOf(payload.getOrDefault("reason", "")),
                String.valueOf(payload.getOrDefault("matchedRuleId", ""))
        );
    }

    private record EvaluateRequest(
            String tenantId,
            String projectId,
            Set<String> roleKeys,
            String platformModelAlias
    ) {
    }
}
