package com.chj.aigc.asset;

import java.util.List;
import java.util.Set;

public final class TenantAssetCatalogService {
    private final AssetCatalogStore store;

    public TenantAssetCatalogService(AssetCatalogStore store) {
        this.store = store;
        seedIfNeeded("tenant-demo");
    }

    public List<Client> clients(String tenantId) {
        return store.listClients(tenantId).stream()
                .filter(Client::active)
                .toList();
    }

    public Client createClient(String clientId, String tenantId, String name) {
        Client client = new Client(clientId, tenantId, name, true);
        store.saveClient(client);
        return client;
    }

    public List<Brand> brands(String tenantId, String clientId) {
        return store.listBrands(tenantId).stream()
                .filter(Brand::active)
                .filter(brand -> clientId == null || brand.clientId().equals(clientId))
                .toList();
    }

    public Brand createBrand(String brandId, String tenantId, String clientId, String name, String summary) {
        boolean clientExists = store.listClients(tenantId).stream()
                .anyMatch(client -> client.id().equals(clientId));
        if (!clientExists) {
            throw new IllegalArgumentException("客户不存在，无法创建品牌");
        }
        Brand brand = new Brand(brandId, tenantId, clientId, name, summary, true);
        store.saveBrand(brand);
        return brand;
    }

    public List<Asset> assets(String tenantId) {
        return store.listAssets(tenantId).stream()
                .filter(Asset::active)
                .toList();
    }

    private void seedIfNeeded(String tenantId) {
        if (store.listClients(tenantId).isEmpty()) {
            store.saveClient(new Client("client-demo", tenantId, "Demo Advertiser", true));
        }
        if (store.listBrands(tenantId).isEmpty()) {
            store.saveBrand(new Brand(
                    "brand-demo",
                    tenantId,
                    "client-demo",
                    "Demo Brand",
                    "Modern skincare brand for urban users",
                    true
            ));
        }
        if (store.listAssets(tenantId).isEmpty()) {
            store.saveAsset(new Asset(
                    "asset-demo-1",
                    tenantId,
                    "project-demo",
                    "client-demo",
                    "brand-demo",
                    "Hero packshot",
                    AssetKind.IMAGE,
                    "oss://demo-assets/hero-packshot.png",
                    Set.of("hero", "product"),
                    true
            ));
        }
    }
}
