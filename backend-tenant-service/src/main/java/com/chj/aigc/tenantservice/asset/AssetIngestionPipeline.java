package com.chj.aigc.tenantservice.asset;

import java.nio.file.Path;

/**
 * 原始文件落盘后的解析入口。
 * 当前实现允许先完成上传链路，后续再接文档切片、向量化和 ES 入库。
 */
public interface AssetIngestionPipeline {
    void ingest(AssetIngestionCommand command);

    record AssetIngestionCommand(
            Asset asset,
            Path storedFile
    ) {
    }
}
