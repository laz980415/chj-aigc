package com.chj.aigc.web;

import com.chj.aigc.access.ModelAccessAdminStore;
import com.chj.aigc.access.ModelAccessDecision;
import com.chj.aigc.access.ModelAccessPolicyEngine;
import com.chj.aigc.access.ModelAccessRequest;
import com.chj.aigc.access.ModelAccessRule;
import java.util.Map;
import java.util.Set;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 平台服务内部接口，供其他微服务做模型访问校验。
 */
@RestController
@RequestMapping("/internal")
public class InternalApiController {
    private final ModelAccessAdminStore modelAccessAdminStore;

    public InternalApiController(ModelAccessAdminStore modelAccessAdminStore) {
        this.modelAccessAdminStore = modelAccessAdminStore;
    }

    @PostMapping("/model-access/decision")
    public ApiResponse<Map<String, Object>> evaluateModelAccess(@RequestBody EvaluateModelAccessRequest request) {
        ModelAccessPolicyEngine engine = new ModelAccessPolicyEngine();
        for (ModelAccessRule rule : modelAccessAdminStore.listRules()) {
            engine.addRule(rule);
        }
        ModelAccessDecision decision = engine.evaluate(new ModelAccessRequest(
                request.tenantId(),
                request.projectId(),
                request.roleKeys() == null ? Set.of() : Set.copyOf(request.roleKeys()),
                request.platformModelAlias()
        ));
        return ApiResponse.success(Map.of(
                "allowed", decision.allowed(),
                "reason", decision.reason(),
                "matchedRuleId", decision.matchedRuleOptional().map(ModelAccessRule::id).orElse(""),
                "matchedScopeType", decision.matchedRuleOptional().map(rule -> rule.scope().type().name()).orElse(""),
                "matchedScopeValue", decision.matchedRuleOptional().map(rule -> rule.scope().value()).orElse("")
        ));
    }

    public record EvaluateModelAccessRequest(
            String tenantId,
            String projectId,
            Set<String> roleKeys,
            String platformModelAlias
    ) {
    }
}
