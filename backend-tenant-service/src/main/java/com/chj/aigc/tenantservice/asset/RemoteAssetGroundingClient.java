package com.chj.aigc.tenantservice.asset;

import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/**
 * 调用模型服务语义检索接口，返回可供品牌 grounding 使用的素材片段。
 */
public final class RemoteAssetGroundingClient implements AssetGroundingClient {
    private final RestClient restClient;

    public RemoteAssetGroundingClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    @SuppressWarnings("unchecked")
    public GroundingContext buildContext(
            String tenantId,
            String brandId,
            String userPrompt,
            List<Asset> assets,
            int limit
    ) {
        Map<String, Object> response = restClient.post()
                .uri("/api/model/assets/grounding-context")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(new BuildContextRequest(
                        tenantId,
                        brandId,
                        userPrompt,
                        assets.stream().map(this::toAssetPayload).toList(),
                        limit
                ))
                .retrieve()
                .body(Map.class);
        List<Map<String, Object>> snippets = response != null && response.get("snippets") instanceof List<?> items
                ? (List<Map<String, Object>>) items
                : List.of();
        return new GroundingContext(
                response == null ? "" : String.valueOf(response.getOrDefault("context_summary", "")),
                snippets.stream().map(this::toSnippet).toList()
        );
    }

    private AssetPayload toAssetPayload(Asset asset) {
        return new AssetPayload(
                asset.id(),
                asset.name(),
                asset.kind().name().toLowerCase(),
                asset.uri(),
                List.copyOf(asset.tags()),
                ""
        );
    }

    private GroundingSnippet toSnippet(Map<String, Object> payload) {
        return new GroundingSnippet(
                String.valueOf(payload.getOrDefault("asset_id", "")),
                String.valueOf(payload.getOrDefault("asset_name", "")),
                String.valueOf(payload.getOrDefault("asset_kind", "")),
                String.valueOf(payload.getOrDefault("source_uri", "")),
                String.valueOf(payload.getOrDefault("content_text", "")),
                String.valueOf(payload.getOrDefault("summary", "")),
                integerValue(payload.get("page_no")),
                integerValue(payload.get("frame_no"))
        );
    }

    private Integer integerValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.valueOf(String.valueOf(value));
    }

    private record BuildContextRequest(
            String tenant_id,
            String brand_id,
            String user_prompt,
            List<AssetPayload> assets,
            int limit
    ) {
    }

    private record AssetPayload(
            String asset_id,
            String name,
            String kind,
            String uri,
            List<String> tags,
            String summary
    ) {
    }
}
