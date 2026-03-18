package com.chj.aigc.tenantservice.asset;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * 通过模型网关触发素材解析、切片和向量索引。
 * 默认不开启，避免在本地没有模型服务时阻塞上传流程。
 */
@Component
@Primary
@ConditionalOnProperty(name = "tenant.asset-upload.ingestion.mode", havingValue = "remote")
public final class RemoteAssetIngestionPipeline implements AssetIngestionPipeline {
    private final RestClient restClient;

    public RemoteAssetIngestionPipeline(
            @Value("${model.service-uri:http://127.0.0.1:8084}") String modelServiceUri
    ) {
        this.restClient = RestClient.builder().baseUrl(modelServiceUri).build();
    }

    @Override
    public void ingest(AssetIngestionCommand command) {
        Asset asset = command.asset();
        restClient.post()
                .uri("/api/model/assets/ingest")
                .body(new IngestRequest(
                        asset.id(),
                        asset.tenantId(),
                        asset.projectId(),
                        asset.brandId(),
                        asset.name(),
                        asset.kind().name().toLowerCase(),
                        asset.uri(),
                        List.copyOf(asset.tags()),
                        ""
                ))
                .retrieve()
                .toBodilessEntity();
    }

    private record IngestRequest(
            String asset_id,
            String tenant_id,
            String project_id,
            String brand_id,
            String name,
            String kind,
            String uri,
            List<String> tags,
            String summary
    ) {
    }
}
