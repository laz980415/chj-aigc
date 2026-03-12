package com.chj.aigc.asset;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class InMemoryAssetCatalogStore implements AssetCatalogStore {
    private final List<Client> clients = new ArrayList<>();
    private final List<Brand> brands = new ArrayList<>();
    private final List<Asset> assets = new ArrayList<>();

    @Override
    public List<Client> listClients(String tenantId) {
        return clients.stream()
                .filter(client -> client.tenantId().equals(tenantId))
                .toList();
    }

    @Override
    public void saveClient(Client client) {
        clients.removeIf(existing -> existing.id().equals(client.id()));
        clients.add(Objects.requireNonNull(client, "client"));
    }

    @Override
    public List<Brand> listBrands(String tenantId) {
        return brands.stream()
                .filter(brand -> brand.tenantId().equals(tenantId))
                .toList();
    }

    @Override
    public void saveBrand(Brand brand) {
        brands.removeIf(existing -> existing.id().equals(brand.id()));
        brands.add(Objects.requireNonNull(brand, "brand"));
    }

    @Override
    public List<Asset> listAssets(String tenantId) {
        return assets.stream()
                .filter(asset -> asset.tenantId().equals(tenantId))
                .toList();
    }

    @Override
    public void saveAsset(Asset asset) {
        assets.removeIf(existing -> existing.id().equals(asset.id()));
        assets.add(Objects.requireNonNull(asset, "asset"));
    }
}
