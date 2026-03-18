package com.chj.aigc.tenantservice.asset;

import org.springframework.stereotype.Component;

/**
 * 当前交付切面仅保证上传与元数据落库，解析链路后续补齐。
 */
@Component
public final class NoOpAssetIngestionPipeline implements AssetIngestionPipeline {
    @Override
    public void ingest(AssetIngestionCommand command) {
        // Intentionally left blank until chunking and embedding are wired in.
    }
}
