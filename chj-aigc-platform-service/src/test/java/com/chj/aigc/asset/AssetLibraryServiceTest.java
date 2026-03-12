package com.chj.aigc.asset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AssetLibraryServiceTest {

    @Test
    void tenantCanManageClientsBrandsRulesAndAssets() {
        AssetLibraryService service = new AssetLibraryService();
        service.addClient(new Client("client-1", "tenant-1", "Brand Owner", true));
        service.addBrand(new Brand("brand-1", "tenant-1", "client-1", "Brand A", "Premium cosmetics", true));
        service.addBrandRule(new BrandRule(
                "rule-1",
                "tenant-1",
                "brand-1",
                BrandRuleKind.FORBIDDEN_STATEMENT,
                "Do not claim medical efficacy",
                true
        ));
        service.addAsset(new Asset(
                "asset-1",
                "tenant-1",
                "project-1",
                "client-1",
                "brand-1",
                "Hero product packshot",
                AssetKind.IMAGE,
                "oss://bucket/brand-a/packshot.png",
                Set.of("hero", "product"),
                true
        ));

        BrandProfile profile = service.loadBrandProfile("tenant-1", "brand-1");
        assertEquals("Brand A", profile.brand().name());
        assertEquals(1, profile.rules().size());
        assertEquals(1, profile.assets().size());
    }

    @Test
    void assetsCanBeFilteredByProjectBrandAndTag() {
        AssetLibraryService service = new AssetLibraryService();
        service.addClient(new Client("client-1", "tenant-1", "Brand Owner", true));
        service.addBrand(new Brand("brand-1", "tenant-1", "client-1", "Brand A", "Premium cosmetics", true));
        service.addAsset(new Asset(
                "asset-1",
                "tenant-1",
                "project-1",
                "client-1",
                "brand-1",
                "Hero image",
                AssetKind.IMAGE,
                "oss://bucket/1.png",
                Set.of("hero", "spring"),
                true
        ));
        service.addAsset(new Asset(
                "asset-2",
                "tenant-1",
                "project-2",
                "client-1",
                "brand-1",
                "Teaser video",
                AssetKind.VIDEO,
                "oss://bucket/2.mp4",
                Set.of("launch"),
                true
        ));

        List<Asset> filtered = service.findAssets(new AssetFilter(
                Optional.of("tenant-1"),
                Optional.of("project-1"),
                Optional.empty(),
                Optional.of("brand-1"),
                Optional.of(AssetKind.IMAGE),
                Set.of("hero")
        ));

        assertEquals(1, filtered.size());
        assertEquals("asset-1", filtered.get(0).id());
    }

    @Test
    void addingAssetForUnknownBrandFails() {
        AssetLibraryService service = new AssetLibraryService();

        assertThrows(IllegalArgumentException.class, () -> service.addAsset(new Asset(
                "asset-1",
                "tenant-1",
                "project-1",
                "client-1",
                "brand-unknown",
                "Orphan asset",
                AssetKind.DOCUMENT,
                "oss://bucket/doc.pdf",
                Set.of(),
                true
        )));
    }

    @Test
    void clientAndBrandQueriesReturnOnlyActiveItems() {
        AssetLibraryService service = new AssetLibraryService();
        service.addClient(new Client("client-1", "tenant-1", "Client 1", true));
        service.addClient(new Client("client-2", "tenant-1", "Client 2", false));
        service.addBrand(new Brand("brand-1", "tenant-1", "client-1", "Brand A", "Summary", true));
        service.addBrand(new Brand("brand-2", "tenant-1", "client-1", "Brand B", "Summary", false));

        assertEquals(1, service.clientsByTenant("tenant-1").size());
        assertEquals(1, service.brandsByClient("tenant-1", "client-1").size());
        assertTrue(service.brandsByClient("tenant-1", "client-1").stream().allMatch(Brand::active));
    }
}
