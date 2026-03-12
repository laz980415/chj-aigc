package com.chj.aigc.tenantservice.asset;

import com.chj.aigc.tenantservice.persistence.RowValueHelper;
import com.chj.aigc.tenantservice.persistence.mapper.AssetCatalogMapper;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 基于 MyBatis XML 的客户、品牌、素材存储实现。
 */
public final class MybatisAssetCatalogStore implements AssetCatalogStore {
    private final AssetCatalogMapper mapper;

    public MybatisAssetCatalogStore(AssetCatalogMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<Client> listClients(String tenantId) {
        return mapper.listClients(tenantId).stream()
                .map(this::mapClient)
                .toList();
    }

    @Override
    public void saveClient(Client client) {
        mapper.upsertClient(Map.of(
                "id", client.id(),
                "tenantId", client.tenantId(),
                "name", client.name(),
                "active", client.active()
        ));
    }

    @Override
    public List<Brand> listBrands(String tenantId) {
        return mapper.listBrands(tenantId).stream()
                .map(this::mapBrand)
                .toList();
    }

    @Override
    public void saveBrand(Brand brand) {
        mapper.upsertBrand(Map.of(
                "id", brand.id(),
                "tenantId", brand.tenantId(),
                "clientId", brand.clientId(),
                "name", brand.name(),
                "summary", brand.summary(),
                "active", brand.active()
        ));
    }

    @Override
    public List<Asset> listAssets(String tenantId) {
        return mapper.listAssets(tenantId).stream()
                .map(this::mapAsset)
                .toList();
    }

    @Override
    public void saveAsset(Asset asset) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", asset.id());
        payload.put("tenantId", asset.tenantId());
        payload.put("projectId", asset.projectId());
        payload.put("clientId", asset.clientId());
        payload.put("brandId", asset.brandId());
        payload.put("name", asset.name());
        payload.put("kind", asset.kind().name());
        payload.put("uri", asset.uri());
        payload.put("tags", String.join(",", asset.tags()));
        payload.put("active", asset.active());
        mapper.upsertAsset(payload);
    }

    private Client mapClient(Map<String, Object> row) {
        return new Client(
                RowValueHelper.string(row, "id"),
                RowValueHelper.string(row, "tenantId", "tenant_id"),
                RowValueHelper.string(row, "name"),
                RowValueHelper.bool(row, "active")
        );
    }

    private Brand mapBrand(Map<String, Object> row) {
        return new Brand(
                RowValueHelper.string(row, "id"),
                RowValueHelper.string(row, "tenantId", "tenant_id"),
                RowValueHelper.string(row, "clientId", "client_id"),
                RowValueHelper.string(row, "name"),
                RowValueHelper.string(row, "summary"),
                RowValueHelper.bool(row, "active")
        );
    }

    private Asset mapAsset(Map<String, Object> row) {
        String tags = RowValueHelper.string(row, "tags");
        Set<String> tagSet = tags == null || tags.isBlank()
                ? Set.of()
                : Arrays.stream(tags.split(","))
                        .map(String::trim)
                        .filter(item -> !item.isBlank())
                        .collect(Collectors.toSet());
        return new Asset(
                RowValueHelper.string(row, "id"),
                RowValueHelper.string(row, "tenantId", "tenant_id"),
                RowValueHelper.string(row, "projectId", "project_id"),
                RowValueHelper.string(row, "clientId", "client_id"),
                RowValueHelper.string(row, "brandId", "brand_id"),
                RowValueHelper.string(row, "name"),
                AssetKind.valueOf(RowValueHelper.string(row, "kind")),
                RowValueHelper.string(row, "uri"),
                tagSet,
                RowValueHelper.bool(row, "active")
        );
    }
}
