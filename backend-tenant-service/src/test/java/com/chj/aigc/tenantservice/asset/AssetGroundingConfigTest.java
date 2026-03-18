package com.chj.aigc.tenantservice.asset;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class AssetGroundingConfigTest {
    @Test
    void configCreatesGroundingBeans() {
        AssetGroundingConfig config = new AssetGroundingConfig();
        AssetGroundingClient client = config.assetGroundingClient("http://127.0.0.1:18084");
        TenantAssetGroundingService service = config.tenantAssetGroundingService(
                new TenantAssetCatalogService(new InMemoryAssetCatalogStore()),
                client
        );

        assertNotNull(service);
        assertInstanceOf(RemoteAssetGroundingClient.class, client);
    }

    private static final class InMemoryAssetCatalogStore implements AssetCatalogStore {
        @Override
        public java.util.List<Client> listClients(String tenantId) {
            return java.util.List.of();
        }

        @Override
        public void saveClient(Client client) {
        }

        @Override
        public java.util.List<Brand> listBrands(String tenantId) {
            return java.util.List.of();
        }

        @Override
        public void saveBrand(Brand brand) {
        }

        @Override
        public java.util.List<Asset> listAssets(String tenantId) {
            return java.util.List.of();
        }

        @Override
        public void saveAsset(Asset asset) {
        }
    }
}
