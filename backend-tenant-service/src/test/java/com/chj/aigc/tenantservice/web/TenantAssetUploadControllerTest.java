package com.chj.aigc.tenantservice.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chj.aigc.tenantservice.asset.AssetCatalogStore;
import com.chj.aigc.tenantservice.asset.AssetIngestionPipeline;
import com.chj.aigc.tenantservice.asset.AssetKind;
import com.chj.aigc.tenantservice.asset.AssetUploadService;
import com.chj.aigc.tenantservice.asset.Brand;
import com.chj.aigc.tenantservice.asset.Client;
import com.chj.aigc.tenantservice.asset.Asset;
import com.chj.aigc.tenantservice.auth.AuthInterceptor;
import com.chj.aigc.tenantservice.auth.AuthSession;
import com.chj.aigc.tenantservice.tenant.TenantProject;
import com.chj.aigc.tenantservice.tenant.TenantProjectStore;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class TenantAssetUploadControllerTest {
    @TempDir
    Path tempDir;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        InMemoryAssetCatalogStore assetCatalogStore = new InMemoryAssetCatalogStore();
        assetCatalogStore.saveBrand(new Brand("brand-demo", "tenant-demo", "client-demo", "演示品牌", "summary", true));
        InMemoryTenantProjectStore tenantProjectStore = new InMemoryTenantProjectStore();
        tenantProjectStore.saveProject(new TenantProject("project-demo", "tenant-demo", "演示项目", true));
        AssetUploadService assetUploadService = new AssetUploadService(
                assetCatalogStore,
                tenantProjectStore,
                command -> {
                },
                tempDir.toString()
        );
        mockMvc = MockMvcBuilders.standaloneSetup(new TenantAssetUploadController(assetUploadService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void tenantOwnerCanUploadAsset() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "storyboard.pdf",
                "application/pdf",
                "document".getBytes()
        );

        mockMvc.perform(multipart("/api/tenant/assets/upload")
                        .file(file)
                        .param("assetId", "asset-upload-ui")
                        .param("projectId", "project-demo")
                        .param("brandId", "brand-demo")
                        .param("name", "故事板")
                        .param("tags", "story")
                        .requestAttr(AuthInterceptor.REQUEST_SESSION_KEY, tenantOwnerSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value("asset-upload-ui"))
                .andExpect(jsonPath("$.data.kind").value(AssetKind.DOCUMENT.name()))
                .andExpect(jsonPath("$.data.name").value("故事板"))
                .andExpect(jsonPath("$.data.uri").exists());
    }

    @Test
    void tenantMemberCannotUploadAsset() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "poster.png",
                "image/png",
                "image".getBytes()
        );

        mockMvc.perform(multipart("/api/tenant/assets/upload")
                        .file(file)
                        .param("projectId", "project-demo")
                        .param("brandId", "brand-demo")
                        .requestAttr(AuthInterceptor.REQUEST_SESSION_KEY, tenantMemberSession()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("当前账号没有执行该租户操作的权限"));
    }

    private AuthSession tenantOwnerSession() {
        return new AuthSession(
                "token-owner",
                "user-tenant-owner",
                "tenant_owner",
                "租户负责人",
                "tenant_owner",
                "tenant-demo",
                Instant.now(),
                Instant.now().plusSeconds(3600)
        );
    }

    private AuthSession tenantMemberSession() {
        return new AuthSession(
                "token-member",
                "user-tenant-member",
                "tenant_member",
                "租户成员",
                "tenant_member",
                "tenant-demo",
                Instant.now(),
                Instant.now().plusSeconds(3600)
        );
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
