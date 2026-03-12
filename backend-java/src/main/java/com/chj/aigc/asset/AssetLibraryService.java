package com.chj.aigc.asset;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class AssetLibraryService {
    private final List<Client> clients = new ArrayList<>();
    private final List<Brand> brands = new ArrayList<>();
    private final List<BrandRule> brandRules = new ArrayList<>();
    private final List<Asset> assets = new ArrayList<>();

    public void addClient(Client client) {
        clients.add(Objects.requireNonNull(client, "client"));
    }

    public void addBrand(Brand brand) {
        clients.stream()
                .filter(client -> client.id().equals(brand.clientId()) && client.tenantId().equals(brand.tenantId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown client for brand"));
        brands.add(Objects.requireNonNull(brand, "brand"));
    }

    public void addBrandRule(BrandRule rule) {
        brands.stream()
                .filter(brand -> brand.id().equals(rule.brandId()) && brand.tenantId().equals(rule.tenantId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown brand for rule"));
        brandRules.add(Objects.requireNonNull(rule, "rule"));
    }

    public void addAsset(Asset asset) {
        brands.stream()
                .filter(brand -> brand.id().equals(asset.brandId()) && brand.tenantId().equals(asset.tenantId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown brand for asset"));
        assets.add(Objects.requireNonNull(asset, "asset"));
    }

    public List<Client> clientsByTenant(String tenantId) {
        return clients.stream()
                .filter(client -> client.tenantId().equals(tenantId) && client.active())
                .toList();
    }

    public List<Brand> brandsByClient(String tenantId, String clientId) {
        return brands.stream()
                .filter(brand -> brand.tenantId().equals(tenantId))
                .filter(brand -> brand.clientId().equals(clientId))
                .filter(Brand::active)
                .toList();
    }

    public List<Asset> findAssets(AssetFilter filter) {
        return assets.stream()
                .filter(Asset::active)
                .filter(asset -> filter.tenantId().map(id -> id.equals(asset.tenantId())).orElse(true))
                .filter(asset -> filter.projectId().map(id -> id.equals(asset.projectId())).orElse(true))
                .filter(asset -> filter.clientId().map(id -> id.equals(asset.clientId())).orElse(true))
                .filter(asset -> filter.brandId().map(id -> id.equals(asset.brandId())).orElse(true))
                .filter(asset -> filter.kind().map(kind -> kind == asset.kind()).orElse(true))
                .filter(asset -> asset.tags().containsAll(filter.requiredTags()))
                .collect(Collectors.toList());
    }

    public BrandProfile loadBrandProfile(String tenantId, String brandId) {
        Brand brand = brands.stream()
                .filter(item -> item.tenantId().equals(tenantId) && item.id().equals(brandId) && item.active())
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Brand not found"));
        List<BrandRule> rules = brandRules.stream()
                .filter(rule -> rule.tenantId().equals(tenantId))
                .filter(rule -> rule.brandId().equals(brandId))
                .filter(BrandRule::active)
                .toList();
        List<Asset> brandAssets = findAssets(AssetFilter.byBrand(tenantId, brandId));
        return new BrandProfile(brand, rules, brandAssets);
    }
}
