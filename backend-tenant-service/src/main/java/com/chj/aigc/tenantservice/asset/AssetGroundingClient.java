package com.chj.aigc.tenantservice.asset;

import java.util.List;

/**
 * 语义素材 grounding 客户端抽象。
 * 后续生成链路只依赖这个接口，不直接感知模型服务的 HTTP 细节。
 */
public interface AssetGroundingClient {
    GroundingContext buildContext(
            String tenantId,
            String brandId,
            String userPrompt,
            List<Asset> assets,
            int limit
    );

    record GroundingContext(
            String contextSummary,
            List<GroundingSnippet> snippets
    ) {
    }

    record GroundingSnippet(
            String assetId,
            String assetName,
            String assetKind,
            String sourceUri,
            String contentText,
            String summary,
            Integer pageNo,
            Integer frameNo
    ) {
    }
}
