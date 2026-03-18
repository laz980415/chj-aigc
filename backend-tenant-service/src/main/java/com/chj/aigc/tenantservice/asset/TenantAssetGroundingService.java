package com.chj.aigc.tenantservice.asset;

import java.util.List;

/**
 * 在租户资产域中整理品牌素材，再委托模型服务做语义 grounding 检索。
 */
public final class TenantAssetGroundingService {
    private final TenantAssetCatalogService tenantAssetCatalogService;
    private final AssetGroundingClient assetGroundingClient;

    public TenantAssetGroundingService(
            TenantAssetCatalogService tenantAssetCatalogService,
            AssetGroundingClient assetGroundingClient
    ) {
        this.tenantAssetCatalogService = tenantAssetCatalogService;
        this.assetGroundingClient = assetGroundingClient;
    }

    public AssetGroundingClient.GroundingContext buildContext(
            String tenantId,
            String brandId,
            String userPrompt,
            int limit
    ) {
        List<Asset> brandAssets = tenantAssetCatalogService.assets(tenantId).stream()
                .filter(asset -> brandId.equals(asset.brandId()))
                .toList();
        return assetGroundingClient.buildContext(tenantId, brandId, userPrompt, brandAssets, limit);
    }
}
