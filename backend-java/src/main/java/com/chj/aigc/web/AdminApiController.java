package com.chj.aigc.web;

import com.chj.aigc.access.AccessScopeType;
import com.chj.aigc.access.ModelAccessAdminStore;
import com.chj.aigc.access.ModelAccessEffect;
import com.chj.aigc.access.ModelAccessAuditEvent;
import com.chj.aigc.access.ModelAccessRule;
import com.chj.aigc.access.ModelAccessScope;
import com.chj.aigc.auth.AuthService;
import com.chj.aigc.auth.AuthUser;
import com.chj.aigc.web.dto.CreateModelAccessRuleRequest;
import com.chj.aigc.web.dto.CreateUserRequest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminApiController {
    private final ModelAccessAdminStore store;
    private final AuthService authService;

    public AdminApiController(ModelAccessAdminStore store, AuthService authService) {
        this.store = store;
        this.authService = authService;
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
    public Map<String, Object> summary() {
        return Map.of(
                "policies", store.listRules().size(),
                "auditEvents", store.listAuditEvents().size(),
                "users", authService.listUsers().size()
        );
    }

    @GetMapping("/roles")
    public List<String> builtinRoles() {
        return authService.builtinRoles();
    }

    @GetMapping("/users")
    public List<Map<String, Object>> users() {
        return authService.listUsers().stream()
                .map(this::serializeUser)
                .toList();
    }

    @PostMapping("/users")
    public Map<String, Object> createUser(@RequestBody CreateUserRequest request) {
        AuthUser user = authService.createUser(
                request.userId(),
                request.username(),
                request.password(),
                request.displayName(),
                request.roleKey(),
                request.tenantId() == null || request.tenantId().isBlank() ? null : request.tenantId()
        );
        return serializeUser(user);
    }

    @GetMapping("/model-access-rules")
    public List<ModelAccessRule> modelAccessRules() {
        return store.listRules();
    }

    @PostMapping("/model-access-rules")
    public ModelAccessRule createModelAccessRule(@RequestBody CreateModelAccessRuleRequest request) {
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
        return rule;
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
}
