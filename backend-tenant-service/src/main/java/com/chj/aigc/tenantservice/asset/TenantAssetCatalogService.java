package com.chj.aigc.tenantservice.asset;

import java.util.List;
import java.util.Set;

/**
 * 租户客户、品牌、素材目录服务。
 */
public final class TenantAssetCatalogService {
    private final AssetCatalogStore store;

    public TenantAssetCatalogService(AssetCatalogStore store) {
        this.store = store;
        seedIfNeeded("tenant-demo");
    }

    /**
     * 返回租户下启用的客户列表。
     */
    public List<Client> clients(String tenantId) {
        return store.listClients(tenantId).stream()
                .filter(Client::active)
                .toList();
    }

    /**
     * 创建客户。
     */
    public Client createClient(String clientId, String tenantId, String name) {
        Client client = new Client(clientId, tenantId, name, true);
        store.saveClient(client);
        return client;
    }

    /**
     * 返回租户品牌列表，可按客户过滤。
     */
    public List<Brand> brands(String tenantId, String clientId) {
        return store.listBrands(tenantId).stream()
                .filter(Brand::active)
                .filter(brand -> clientId == null || brand.clientId().equals(clientId))
                .toList();
    }

    /**
     * 创建品牌，要求客户已存在。
     */
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

    /**
     * 返回租户启用素材列表。
     */
    public List<Asset> assets(String tenantId) {
        return store.listAssets(tenantId).stream()
                .filter(Asset::active)
                .toList();
    }

    private void seedIfNeeded(String tenantId) {
        if (store.listClients(tenantId).isEmpty()) {
            store.saveClient(new Client("client-demo", tenantId, "演示广告主", true));
        }
        if (store.listBrands(tenantId).isEmpty()) {
            store.saveBrand(new Brand(
                    "brand-demo",
                    tenantId,
                    "client-demo",
                    "演示品牌",
                    "面向城市白领的护肤品牌",
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
                    "演示主视觉",
                    AssetKind.IMAGE,
                    "oss://demo-assets/hero-packshot.png",
                    Set.of("hero", "product"),
                    true
            ));
        }
    }
}
