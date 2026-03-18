package com.chj.aigc.web;

import com.chj.aigc.access.AccessScopeType;
import com.chj.aigc.access.ModelAccessAdminStore;
import com.chj.aigc.access.ModelAccessEffect;
import com.chj.aigc.access.ModelAccessAuditEvent;
import com.chj.aigc.access.ModelAccessRule;
import com.chj.aigc.access.ModelAccessScope;
import com.chj.aigc.auth.PlatformAuthService;
import com.chj.aigc.auth.AuthUser;
import com.chj.aigc.billing.TenantBillingService;
import com.chj.aigc.provider.ProviderConfig;
import com.chj.aigc.provider.ProviderConfigStore;
import com.chj.aigc.web.dto.CreateModelAccessRuleRequest;
import com.chj.aigc.web.dto.CreateUserRequest;
import com.chj.aigc.web.dto.UpsertProviderConfigRequest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

/**
 * 平台超管 API。
 * 负责平台账号管理、模型访问策略维护和供应商配置管理。
 */
@RestController
@RequestMapping("/api/admin")
public class AdminApiController {
    private final ModelAccessAdminStore store;
    private final PlatformAuthService authService;
    private final TenantBillingService tenantBillingService;
    private final ProviderConfigStore providerConfigStore;

    public AdminApiController(ModelAccessAdminStore store, PlatformAuthService authService,
                              TenantBillingService tenantBillingService, ProviderConfigStore providerConfigStore) {
        this.store = store;
        this.authService = authService;
        this.tenantBillingService = tenantBillingService;
        this.providerConfigStore = providerConfigStore;
        if (store.listRules().isEmpty()) {
            ModelAccessRule seedRule = new ModelAccessRule(
                    "rule-seed-1", "copy-standard",
                    new ModelAccessScope(AccessScopeType.TENANT, "tenant-demo"),
                    ModelAccessEffect.ALLOW, true, "system", Instant.now(), "Seed rule for demo tenant"
            );
            store.saveRule(seedRule);
            store.saveAuditEvent(new ModelAccessAuditEvent(
                    "audit-seed-1", "system", "RULE_CREATED",
                    seedRule.platformModelAlias(), seedRule.scope().type().name(),
                    seedRule.scope().value(), seedRule.reason(), Instant.now()
            ));
        }
    }

    @GetMapping("/summary")
    public ApiResponse<Map<String, Object>> summary() {
        return ApiResponse.success(Map.of(
                "policies", store.listRules().size(),
                "auditEvents", store.listAuditEvents().size(),
                "users", authService.listUsers().size()
        ));
    }

    @GetMapping("/roles")
    public ApiResponse<List<String>> builtinRoles() {
        return ApiResponse.success(authService.builtinRoles());
    }

    @GetMapping("/users")
    public ApiResponse<List<Map<String, Object>>> users() {
        return ApiResponse.success(authService.listUsers().stream().map(this::serializeUser).toList());
    }

    @GetMapping("/tenants")
    public ApiResponse<List<Map<String, Object>>> tenants() {
        Map<String, List<AuthUser>> tenantUsers = new LinkedHashMap<>();
        authService.listUsers().stream()
                .filter(user -> user.tenantId() != null && !user.tenantId().isBlank())
                .forEach(user -> tenantUsers.computeIfAbsent(user.tenantId(), ignored -> new ArrayList<>()).add(user));
        return ApiResponse.success(tenantUsers.entrySet().stream()
                .map(entry -> serializeTenant(entry.getKey(), entry.getValue())).toList());
    }

    @GetMapping("/tenants/{tenantId}")
    public ApiResponse<Map<String, Object>> tenantDetail(@PathVariable String tenantId) {
        List<AuthUser> tenantUsers = authService.listTenantUsers(tenantId);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tenant", serializeTenant(tenantId, tenantUsers));
        payload.put("members", tenantUsers.stream().map(this::serializeUser).toList());
        payload.put("rules", store.listRules().stream()
                .filter(rule -> rule.scope().type() == AccessScopeType.TENANT)
                .filter(rule -> tenantId.equals(rule.scope().value())).toList());
        payload.put("ledgerEntries", tenantBillingService.ledgerEntries(tenantId));
        payload.put("paymentOrders", tenantBillingService.paymentOrders(tenantId));
        return ApiResponse.success(payload);
    }

    @PostMapping("/users")
    public ApiResponse<Map<String, Object>> createUser(@RequestBody CreateUserRequest request) {
        AuthUser user = authService.createUser(
                request.userId(), request.username(), request.password(), request.displayName(),
                request.roleKey(), request.tenantId() == null || request.tenantId().isBlank() ? null : request.tenantId()
        );
        return ApiResponse.success(serializeUser(user));
    }

    @GetMapping("/model-access-rules")
    public ApiResponse<List<ModelAccessRule>> modelAccessRules() {
        return ApiResponse.success(store.listRules());
    }

    @PostMapping("/model-access-rules")
    public ApiResponse<ModelAccessRule> createModelAccessRule(@RequestBody CreateModelAccessRuleRequest request) {
        ModelAccessRule rule = new ModelAccessRule(
                request.ruleId(), request.platformModelAlias(),
                new ModelAccessScope(AccessScopeType.valueOf(request.scopeType().toUpperCase()), request.scopeValue()),
                ModelAccessEffect.valueOf(request.effect().toUpperCase()), true,
                request.actorId(), Instant.now(), request.reason()
        );
        store.saveRule(rule);
        store.saveAuditEvent(new ModelAccessAuditEvent(
                "audit-" + request.ruleId(), request.actorId(), "RULE_CREATED",
                rule.platformModelAlias(), rule.scope().type().name(),
                rule.scope().value(), rule.reason(), Instant.now()
        ));
        return ApiResponse.success(rule);
    }

    // ── 供应商配置管理 ────────────────────────────────────────────────────

    /** 列出所有供应商配置（API Key 脱敏返回）。 */
    @GetMapping("/provider-configs")
    public ApiResponse<List<Map<String, Object>>> listProviderConfigs() {
        return ApiResponse.success(providerConfigStore.listAll().stream()
                .map(this::serializeProviderConfig).toList());
    }

    /** 新增或更新供应商配置。 */
    @PostMapping("/provider-configs")
    public ApiResponse<Map<String, Object>> upsertProviderConfig(@RequestBody UpsertProviderConfigRequest req) {
        ProviderConfig config = new ProviderConfig(
                UUID.randomUUID().toString(),
                req.providerId(),
                req.displayName(),
                req.apiBaseUrl(),
                req.apiKey(),
                req.enabled(),
                req.updatedBy() != null ? req.updatedBy() : "admin",
                Instant.now()
        );
        providerConfigStore.save(config);
        return ApiResponse.success(serializeProviderConfig(config));
    }

    /** 启用或停用供应商。 */
    @PostMapping("/provider-configs/{providerId}/enabled")
    public ApiResponse<Void> setProviderEnabled(@PathVariable String providerId,
                                                 @RequestBody Map<String, Boolean> body) {
        providerConfigStore.setEnabled(providerId, Boolean.TRUE.equals(body.get("enabled")));
        return ApiResponse.success(null);
    }

    /** 供应商配置查询接口，供模型网关拉取（不脱敏）。 */
    @GetMapping("/provider-configs/internal")
    public ApiResponse<List<ProviderConfig>> internalProviderConfigs() {
        return ApiResponse.success(providerConfigStore.listAll().stream()
                .filter(ProviderConfig::enabled).toList());
    }

    // ── 私有序列化方法 ────────────────────────────────────────────────────

    private Map<String, Object> serializeProviderConfig(ProviderConfig c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("providerId", c.providerId());
        m.put("displayName", c.displayName());
        m.put("apiBaseUrl", c.apiBaseUrl());
        m.put("apiKeyMasked", c.apiKey().length() > 8
                ? c.apiKey().substring(0, 4) + "****" + c.apiKey().substring(c.apiKey().length() - 4)
                : "****");
        m.put("enabled", c.enabled());
        m.put("updatedBy", c.updatedBy());
        m.put("updatedAt", c.updatedAt());
        return m;
    }

    private Map<String, Object> serializeUser(AuthUser user) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", user.id());
        payload.put("username", user.username());
        payload.put("displayName", user.displayName());
        payload.put("roleKey", user.roleKey());
        payload.put("tenantId", user.tenantId());
        payload.put("active", user.active());
        return payload;
    }

    private Map<String, Object> serializeTenant(String tenantId, List<AuthUser> tenantUsers) {
        Map<String, Object> wallet = tenantBillingService.walletSnapshot(tenantId);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tenantId", tenantId);
        payload.put("displayName", tenantId);
        payload.put("memberCount", tenantUsers.size());
        payload.put("ownerCount", tenantUsers.stream().filter(u -> "tenant_owner".equals(u.roleKey())).count());
        payload.put("activeMemberCount", tenantUsers.stream().filter(AuthUser::active).count());
        payload.put("walletBalance", wallet.get("balance"));
        payload.put("ledgerCount", wallet.get("ledgerCount"));
        return payload;
    }
}
