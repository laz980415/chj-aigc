package com.chj.aigc.tenantservice.asset;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * 资产上传与语义 grounding 相关组件装配。
 * 独立配置文件用于避免与已有租户服务热配置文件冲突。
 */
@Configuration
public class AssetGroundingConfig {
    @Bean
    public AssetGroundingClient assetGroundingClient(
            @Value("${model.service-uri:http://127.0.0.1:8084}") String modelServiceUri
    ) {
        return new RemoteAssetGroundingClient(RestClient.builder().baseUrl(modelServiceUri).build());
    }

    @Bean
    public TenantAssetGroundingService tenantAssetGroundingService(
            TenantAssetCatalogService tenantAssetCatalogService,
            AssetGroundingClient assetGroundingClient
    ) {
        return new TenantAssetGroundingService(tenantAssetCatalogService, assetGroundingClient);
    }
}
