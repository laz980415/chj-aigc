package com.chj.aigc.asset;

import java.util.List;
import java.util.Objects;

public record BrandProfile(
        Brand brand,
        List<BrandRule> rules,
        List<Asset> assets
) {
    public BrandProfile {
        Objects.requireNonNull(brand, "brand");
        Objects.requireNonNull(rules, "rules");
        Objects.requireNonNull(assets, "assets");
        rules = List.copyOf(rules);
        assets = List.copyOf(assets);
    }
}
