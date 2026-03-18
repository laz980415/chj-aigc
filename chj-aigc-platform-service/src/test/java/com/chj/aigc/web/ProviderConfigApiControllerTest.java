package com.chj.aigc.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.chj.aigc.access.AccessScopeType;
import com.chj.aigc.access.InMemoryModelAccessAdminStore;
import com.chj.aigc.access.ModelAccessEffect;
import com.chj.aigc.access.ModelAccessRule;
import com.chj.aigc.access.ModelAccessScope;
import com.chj.aigc.billing.TenantBillingService;
import com.chj.aigc.provider.ProviderConfig;
import com.chj.aigc.provider.ProviderConfigStore;
import com.chj.aigc.auth.PlatformAuthService;
import com.chj.aigc.web.dto.UpsertProviderConfigRequest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ProviderConfigApiControllerTest {
    @Test
    void adminControllerCanSaveListAndFilterEnabledProviderConfigs() {
        InMemoryProviderConfigStore providerConfigStore = new InMemoryProviderConfigStore();
        AdminApiController controller = new AdminApiController(
                new InMemoryModelAccessAdminStore(),
                mock(PlatformAuthService.class),
                mock(TenantBillingService.class),
                providerConfigStore
        );

        ApiResponse<Map<String, Object>> saved = controller.upsertProviderConfig(new UpsertProviderConfigRequest(
                "openai",
                "OpenAI Local",
                "https://api.openai.com/v1",
                "sk-local-demo-1234",
                true,
                "unit-test"
        ));

        assertEquals("openai", saved.data().get("providerId"));
        assertEquals("sk-l****1234", saved.data().get("apiKeyMasked"));

        ApiResponse<List<Map<String, Object>>> listed = controller.listProviderConfigs();
        assertEquals(1, listed.data().size());
        assertEquals("OpenAI Local", listed.data().getFirst().get("displayName"));

        ApiResponse<List<ProviderConfig>> internal = controller.internalProviderConfigs();
        assertEquals(1, internal.data().size());
        assertEquals("sk-local-demo-1234", internal.data().getFirst().apiKey());

        controller.setProviderEnabled("openai", Map.of("enabled", false));
        assertFalse(controller.internalProviderConfigs().data().stream().anyMatch(item -> "openai".equals(item.providerId())));
    }

    @Test
    void internalApiControllerReturnsAllowDecisionForMatchingRule() {
        InMemoryModelAccessAdminStore store = new InMemoryModelAccessAdminStore();
        store.saveRule(new ModelAccessRule(
                "rule-demo",
                "copy-standard",
                new ModelAccessScope(AccessScopeType.TENANT, "tenant-demo"),
                ModelAccessEffect.ALLOW,
                true,
                "unit-test",
                Instant.now(),
                "allow demo tenant"
        ));
        InternalApiController controller = new InternalApiController(store);

        ApiResponse<Map<String, Object>> response = controller.evaluateModelAccess(
                new InternalApiController.EvaluateModelAccessRequest(
                        "tenant-demo",
                        "project-demo",
                        Set.of("tenant_owner"),
                        "copy-standard"
                )
        );

        assertEquals(Boolean.TRUE, response.data().get("allowed"));
        assertEquals("rule-demo", response.data().get("matchedRuleId"));
        assertEquals("TENANT", response.data().get("matchedScopeType"));
        assertEquals("tenant-demo", response.data().get("matchedScopeValue"));
    }

    private static final class InMemoryProviderConfigStore implements ProviderConfigStore {
        private final Map<String, ProviderConfig> storage = new LinkedHashMap<>();

        @Override
        public List<ProviderConfig> listAll() {
            return new ArrayList<>(storage.values());
        }

        @Override
        public Optional<ProviderConfig> findByProviderId(String providerId) {
            return Optional.ofNullable(storage.get(providerId));
        }

        @Override
        public void save(ProviderConfig config) {
            storage.put(config.providerId(), config);
        }

        @Override
        public void setEnabled(String providerId, boolean enabled) {
            ProviderConfig existing = storage.get(providerId);
            if (existing == null) {
                return;
            }
            storage.put(providerId, new ProviderConfig(
                    existing.id(),
                    existing.providerId(),
                    existing.displayName(),
                    existing.apiBaseUrl(),
                    existing.apiKey(),
                    enabled,
                    existing.updatedBy(),
                    existing.updatedAt()
            ));
        }
    }
}
