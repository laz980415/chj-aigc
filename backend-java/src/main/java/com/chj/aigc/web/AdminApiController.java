package com.chj.aigc.web;

import com.chj.aigc.access.AccessScopeType;
import com.chj.aigc.access.ModelAccessAdminStore;
import com.chj.aigc.access.ModelAccessEffect;
import com.chj.aigc.access.ModelAccessAuditEvent;
import com.chj.aigc.access.ModelAccessRule;
import com.chj.aigc.access.ModelAccessScope;
import com.chj.aigc.auth.AuthService;
import com.chj.aigc.auth.AuthUser;
import com.chj.aigc.billing.TenantBillingService;
import com.chj.aigc.web.dto.CreateModelAccessRuleRequest;
import com.chj.aigc.web.dto.CreateUserRequest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 平台超管 API。
 * 负责平台账号管理和模型访问策略维护，不处理租户内部项目和成员运营。
 */
@RestController
@RequestMapping("/api/admin")
public class AdminApiController {
    private final ModelAccessAdminStore store;
    private final AuthService authService;
    private final TenantBillingService tenantBillingService;

    public AdminApiController(ModelAccessAdminStore store, AuthService authService, TenantBillingService tenantBillingService) {
        this.store = store;
        this.authService = authService;
        this.tenantBillingService = tenantBillingService;
        if (store.listRules().isEmpty()) {
            ModelAccessRule seedRule = new ModelAccessRule(
                    "rule-seed-1",
                    "copy-standard",
                    new ModelAccessScope(AccessScopeType.TENANT, "tenant-demo"),
                    ModelAccessEffect.ALLOW,
                    true,
                    "system",
                    Instant.now(),
                    "Seed rule for demo tenant"
            );
            store.saveRule(seedRule);
            store.saveAuditEvent(new ModelAccessAuditEvent(
                    "audit-seed-1",
                    "system",
                    "RULE_CREATED",
                    seedRule.platformModelAlias(),
                    seedRule.scope().type().name(),
                    seedRule.scope().value(),
                    seedRule.reason(),
                    Instant.now()
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
        return ApiResponse.success(authService.listUsers().stream()
                .map(this::serializeUser)
                .toList());
    }

    /**
     * 返回平台侧租户总览。
     * 当前根据账号中的 tenantId 聚合租户，并补充钱包余额和成员数量，供超管工作台展示。
     */
    @GetMapping("/tenants")
    public ApiResponse<List<Map<String, Object>>> tenants() {
        Map<String, List<AuthUser>> tenantUsers = new LinkedHashMap<>();
        authService.listUsers().stream()
                .filter(user -> user.tenantId() != null && !user.tenantId().isBlank())
                .forEach(user -> tenantUsers.computeIfAbsent(user.tenantId(), ignored -> new ArrayList<>()).add(user));

        List<Map<String, Object>> payload = tenantUsers.entrySet().stream()
                .map(entry -> serializeTenant(entry.getKey(), entry.getValue()))
                .toList();
        return ApiResponse.success(payload);
    }

    /**
     * 返回单个租户详情。
     * 平台超管只查看租户级信息，包括成员摘要、模型策略和钱包流水，不下钻项目内部结构。
     */
    @GetMapping("/tenants/{tenantId}")
    public ApiResponse<Map<String, Object>> tenantDetail(@PathVariable String tenantId) {
        List<AuthUser> tenantUsers = authService.listTenantUsers(tenantId);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tenant", serializeTenant(tenantId, tenantUsers));
        payload.put("members", tenantUsers.stream().map(this::serializeUser).toList());
        payload.put("rules", store.listRules().stream()
                .filter(rule -> rule.scope().type() == AccessScopeType.TENANT)
                .filter(rule -> tenantId.equals(rule.scope().value()))
                .toList());
        payload.put("ledgerEntries", tenantBillingService.ledgerEntries(tenantId));
        return ApiResponse.success(payload);
    }

    @PostMapping("/users")
    public ApiResponse<Map<String, Object>> createUser(@RequestBody CreateUserRequest request) {
        AuthUser user = authService.createUser(
                request.userId(),
                request.username(),
                request.password(),
                request.displayName(),
                request.roleKey(),
                request.tenantId() == null || request.tenantId().isBlank() ? null : request.tenantId()
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
                request.ruleId(),
                request.platformModelAlias(),
                new ModelAccessScope(
                        AccessScopeType.valueOf(request.scopeType().toUpperCase()),
                        request.scopeValue()
                ),
                ModelAccessEffect.valueOf(request.effect().toUpperCase()),
                true,
                request.actorId(),
                Instant.now(),
                request.reason()
        );
        store.saveRule(rule);
        store.saveAuditEvent(new ModelAccessAuditEvent(
                "audit-" + request.ruleId(),
                request.actorId(),
                "RULE_CREATED",
                rule.platformModelAlias(),
                rule.scope().type().name(),
                rule.scope().value(),
                rule.reason(),
                Instant.now()
        ));
        return ApiResponse.success(rule);
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
        long ownerCount = tenantUsers.stream()
                .filter(user -> "tenant_owner".equals(user.roleKey()))
                .count();
        long activeUsers = tenantUsers.stream()
                .filter(AuthUser::active)
                .count();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tenantId", tenantId);
        payload.put("displayName", tenantId);
        payload.put("memberCount", tenantUsers.size());
        payload.put("ownerCount", ownerCount);
        payload.put("activeMemberCount", activeUsers);
        payload.put("walletBalance", wallet.get("balance"));
        payload.put("ledgerCount", wallet.get("ledgerCount"));
        return payload;
    }
}
