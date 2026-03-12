package com.chj.aigc.tenantservice.persistence.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

/**
 * 客户、品牌、素材 MyBatis 映射接口。
 */
public interface AssetCatalogMapper {
    List<Map<String, Object>> listClients(@Param("tenantId") String tenantId);

    void upsertClient(Map<String, Object> client);

    List<Map<String, Object>> listBrands(@Param("tenantId") String tenantId);

    void upsertBrand(Map<String, Object> brand);

    List<Map<String, Object>> listAssets(@Param("tenantId") String tenantId);

    void upsertAsset(Map<String, Object> asset);
}
