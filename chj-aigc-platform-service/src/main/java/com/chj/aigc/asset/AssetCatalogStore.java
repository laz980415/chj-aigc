package com.chj.aigc.asset;

import java.util.List;

public interface AssetCatalogStore {
    List<Client> listClients(String tenantId);

    void saveClient(Client client);

    List<Brand> listBrands(String tenantId);

    void saveBrand(Brand brand);

    List<Asset> listAssets(String tenantId);

    void saveAsset(Asset asset);
}
