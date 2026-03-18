package com.chj.aigc.tenantservice.generation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;

/**
 * 调用 Python 模型网关提交和查询生成任务。
 */
public final class RemoteModelGatewayClient implements ModelGatewayClient {
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final HttpClient httpClient;
    private final URI baseUri;
    private final ObjectMapper objectMapper;

    public RemoteModelGatewayClient(String modelServiceUri, ObjectMapper objectMapper) {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        this.baseUri = URI.create(modelServiceUri);
        this.objectMapper = objectMapper;
    }

    @Override
    public JobResult submitJob(SubmitPayload payload) {
        SubmitJobRequest request = new SubmitJobRequest(
                payload.tenantId(),
                payload.projectId(),
                payload.actorId(),
                payload.modelAlias(),
                payload.capability(),
                payload.userPrompt(),
                payload.brandName(),
                payload.brandSummary(),
                payload.clientName(),
                payload.assets().stream()
                        .map(item -> new AssetRequest(
                                item.id(),
                                item.name(),
                                item.kind(),
                                item.uri(),
                                item.tags(),
                                item.summary()
                        ))
                        .toList(),
                null
        );
        return mapResult(sendJson("/api/model/jobs", "POST", writeJson(request)));
    }

    @Override
    public JobResult fetchJob(String jobId) {
        return mapResult(sendJson("/api/model/jobs/" + jobId, "GET", null));
    }

    @SuppressWarnings("unchecked")
    private JobResult mapResult(Map<String, Object> envelope) {
        Map<String, Object> payload = envelope != null && envelope.get("data") instanceof Map<?, ?> data
                ? (Map<String, Object>) data
                : envelope;
        return new JobResult(
                stringValue(payload, "job_id"),
                stringValue(payload, "status"),
                stringValue(payload, "output_text"),
                stringValue(payload, "output_uri"),
                stringValue(payload, "error_message"),
                stringValue(payload, "provider_id"),
                stringValue(payload, "provider_model_name"),
                stringValue(payload, "provider_job_id"),
                integerValue(payload, "input_tokens"),
                integerValue(payload, "output_tokens"),
                integerValue(payload, "image_count"),
                integerValue(payload, "video_seconds")
        );
    }

    private String stringValue(Map<String, Object> payload, String key) {
        Object value = payload == null ? null : payload.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private Integer integerValue(Map<String, Object> payload, String key) {
        Object value = payload == null ? null : payload.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.valueOf(String.valueOf(value));
    }

    private String writeJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("模型任务请求序列化失败", exception);
        }
    }

    private Map<String, Object> sendJson(String path, String method, String requestBody) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(baseUri.resolve(path))
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
            if ("POST".equals(method)) {
                builder.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody == null ? "" : requestBody));
            } else {
                builder.GET();
            }

            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw HttpClientErrorException.create(
                        HttpStatusCode.valueOf(response.statusCode()),
                        response.body(),
                        HttpHeaders.EMPTY,
                        response.body().getBytes(),
                        null
                );
            }
            return objectMapper.readValue(response.body(), MAP_TYPE);
        } catch (IOException exception) {
            throw new IllegalStateException("模型服务响应解析失败", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("模型服务请求被中断", exception);
        }
    }

    private record SubmitJobRequest(
            String tenant_id,
            String project_id,
            String actor_id,
            String model_alias,
            String capability,
            String user_prompt,
            String brand_name,
            String brand_summary,
            String client_name,
            List<AssetRequest> assets,
            String trace_id
    ) {
    }

    private record AssetRequest(
            String id,
            String name,
            String kind,
            String uri,
            List<String> tags,
            String summary
    ) {
    }
}
