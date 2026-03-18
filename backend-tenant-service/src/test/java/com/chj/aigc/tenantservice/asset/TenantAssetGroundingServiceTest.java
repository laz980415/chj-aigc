package com.chj.aigc.tenantservice.asset;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TenantAssetGroundingServiceTest {
    @Test
    void serviceOnlySendsAssetsForSelectedBrand() {
        InMemoryAssetCatalogStore store = new InMemoryAssetCatalogStore();
        store.saveClient(new Client("client-demo", "tenant-demo", "演示客户", true));
        store.saveBrand(new Brand("brand-a", "tenant-demo", "client-demo", "品牌 A", "", true));
        store.saveBrand(new Brand("brand-b", "tenant-demo", "client-demo", "品牌 B", "", true));
        store.saveAsset(new Asset(
                "asset-a-1",
                "tenant-demo",
                "project-demo",
                "client-demo",
                "brand-a",
                "A 素材",
                AssetKind.IMAGE,
                "E:/uploads/a.png",
                Set.of("hero"),
                true
        ));
        store.saveAsset(new Asset(
                "asset-b-1",
                "tenant-demo",
                "project-demo",
                "client-demo",
                "brand-b",
                "B 素材",
                AssetKind.IMAGE,
                "E:/uploads/b.png",
                Set.of("hero"),
                true
        ));
        RecordingGroundingClient client = new RecordingGroundingClient();
        TenantAssetGroundingService service = new TenantAssetGroundingService(
                new TenantAssetCatalogService(store),
                client
        );

        service.buildContext("tenant-demo", "brand-a", "生成新品文案", 3);

        assertEquals(1, client.lastAssets.size());
        assertEquals("asset-a-1", client.lastAssets.get(0).id());
    }

    private static final class RecordingGroundingClient implements AssetGroundingClient {
        private List<Asset> lastAssets = List.of();

        @Override
        public GroundingContext buildContext(String tenantId, String brandId, String userPrompt, List<Asset> assets, int limit) {
            this.lastAssets = List.copyOf(assets);
            return new GroundingContext("", List.of());
        }
    }

    private static final class InMemoryAssetCatalogStore implements AssetCatalogStore {
        private final List<Client> clients = new ArrayList<>();
        private final List<Brand> brands = new ArrayList<>();
        private final List<Asset> assets = new ArrayList<>();

        @Override
        public List<Client> listClients(String tenantId) {
            return clients.stream().filter(client -> tenantId.equals(client.tenantId())).toList();
        }

        @Override
        public void saveClient(Client client) {
            clients.add(client);
        }

        @Override
        public List<Brand> listBrands(String tenantId) {
            return brands.stream().filter(brand -> tenantId.equals(brand.tenantId())).toList();
        }

        @Override
        public void saveBrand(Brand brand) {
            brands.add(brand);
        }

        @Override
        public List<Asset> listAssets(String tenantId) {
            return assets.stream().filter(asset -> tenantId.equals(asset.tenantId())).toList();
        }

        @Override
        public void saveAsset(Asset asset) {
            assets.add(asset);
        }
    }
}
