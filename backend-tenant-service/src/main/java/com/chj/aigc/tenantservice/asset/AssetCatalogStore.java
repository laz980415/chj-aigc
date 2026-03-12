package com.chj.aigc.tenantservice.asset;

import java.util.List;

/**
 * 客户、品牌、素材存储抽象。
 */
public interface AssetCatalogStore {
    List<Client> listClients(String tenantId);

    void saveClient(Client client);

    List<Brand> listBrands(String tenantId);

    void saveBrand(Brand brand);

    List<Asset> listAssets(String tenantId);

    void saveAsset(Asset asset);
}
