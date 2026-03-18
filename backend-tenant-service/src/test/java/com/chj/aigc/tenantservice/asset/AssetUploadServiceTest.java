package com.chj.aigc.tenantservice.asset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.chj.aigc.tenantservice.tenant.TenantProject;
import com.chj.aigc.tenantservice.tenant.TenantProjectStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class AssetUploadServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void uploadStoresFilePersistsMetadataAndTriggersIngestion() throws Exception {
        InMemoryAssetCatalogStore assetCatalogStore = new InMemoryAssetCatalogStore();
        assetCatalogStore.saveBrand(new Brand("brand-demo", "tenant-demo", "client-demo", "演示品牌", "summary", true));
        InMemoryTenantProjectStore tenantProjectStore = new InMemoryTenantProjectStore();
        tenantProjectStore.saveProject(new TenantProject("project-demo", "tenant-demo", "演示项目", true));
        RecordingAssetIngestionPipeline ingestionPipeline = new RecordingAssetIngestionPipeline();
        AssetUploadService service = new AssetUploadService(
                assetCatalogStore,
                tenantProjectStore,
                ingestionPipeline,
                tempDir.toString()
        );

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "poster.png",
                "image/png",
                "fake-image".getBytes()
        );

        Asset asset = service.upload("tenant-demo", new AssetUploadService.UploadAssetCommand(
                "asset-upload-test",
                "project-demo",
                "brand-demo",
                "春季海报",
                null,
                List.of("hero", "launch"),
                file
        ));

        assertEquals("asset-upload-test", asset.id());
        assertEquals(AssetKind.IMAGE, asset.kind());
        assertEquals("春季海报", asset.name());
        assertTrue(asset.tags().contains("hero"));
        assertTrue(Files.exists(Path.of(asset.uri())));
        assertNotNull(ingestionPipeline.lastCommand);
        assertEquals(asset.id(), ingestionPipeline.lastCommand.asset().id());
        assertEquals("fake-image", Files.readString(ingestionPipeline.lastCommand.storedFile()));
    }

    @Test
    void uploadRejectsUnknownProject() {
        InMemoryAssetCatalogStore assetCatalogStore = new InMemoryAssetCatalogStore();
        assetCatalogStore.saveBrand(new Brand("brand-demo", "tenant-demo", "client-demo", "演示品牌", "summary", true));
        AssetUploadService service = new AssetUploadService(
                assetCatalogStore,
                new InMemoryTenantProjectStore(),
                new RecordingAssetIngestionPipeline(),
                tempDir.toString()
        );
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "guide.pdf",
                "application/pdf",
                "doc".getBytes()
        );

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> service.upload(
                "tenant-demo",
                new AssetUploadService.UploadAssetCommand(
                        "asset-upload-test",
                        "project-missing",
                        "brand-demo",
                        null,
                        null,
                        List.of(),
                        file
                )
        ));

        assertEquals("项目不存在，无法上传素材", exception.getMessage());
    }

    private static final class RecordingAssetIngestionPipeline implements AssetIngestionPipeline {
        private AssetIngestionCommand lastCommand;

        @Override
        public void ingest(AssetIngestionCommand command) {
            this.lastCommand = command;
        }
    }

    private static final class InMemoryTenantProjectStore implements TenantProjectStore {
        private final List<TenantProject> projects = new ArrayList<>();

        @Override
        public List<TenantProject> listProjects(String tenantId) {
            return projects.stream().filter(project -> tenantId.equals(project.tenantId())).toList();
        }

        @Override
        public void saveProject(TenantProject project) {
            projects.add(project);
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
