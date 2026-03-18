package com.chj.aigc.provider;

import java.util.List;
import java.util.Optional;

public interface ProviderConfigStore {
    List<ProviderConfig> listAll();
    Optional<ProviderConfig> findByProviderId(String providerId);
    void save(ProviderConfig config);
    void setEnabled(String providerId, boolean enabled);
}
