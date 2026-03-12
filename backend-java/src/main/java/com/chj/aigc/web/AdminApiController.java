package com.chj.aigc.web;

import com.chj.aigc.access.AccessScopeType;
import com.chj.aigc.access.ModelAccessEffect;
import com.chj.aigc.access.ModelAccessPolicyEngine;
import com.chj.aigc.access.ModelAccessRule;
import com.chj.aigc.access.ModelAccessScope;
import com.chj.aigc.web.dto.CreateModelAccessRuleRequest;
import java.time.Instant;
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
    private final ModelAccessPolicyEngine policyEngine;

    public AdminApiController(ModelAccessPolicyEngine policyEngine) {
        this.policyEngine = policyEngine;
        if (policyEngine.rules().isEmpty()) {
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
            policyEngine.addRule(seedRule);
            policyEngine.recordRuleCreated("audit-seed-1", "system", seedRule);
        }
    }

    @GetMapping("/summary")
    public Map<String, Object> summary() {
        return Map.of(
                "policies", policyEngine.rules().size(),
                "auditEvents", policyEngine.auditEvents().size()
        );
    }

    @GetMapping("/model-access-rules")
    public List<ModelAccessRule> modelAccessRules() {
        return policyEngine.rules();
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
        policyEngine.addRule(rule);
        policyEngine.recordRuleCreated("audit-" + request.ruleId(), request.actorId(), rule);
        return rule;
    }
}
