package com.chj.aigc.tenantservice.web;

import com.chj.aigc.tenantservice.auth.AuthInterceptor;
import com.chj.aigc.tenantservice.auth.AuthService;
import com.chj.aigc.tenantservice.auth.AuthSession;
import com.chj.aigc.tenantservice.auth.AuthUser;
import com.chj.aigc.tenantservice.billing.QuotaAllocation;
import com.chj.aigc.tenantservice.billing.QuotaDimension;
import com.chj.aigc.tenantservice.billing.QuotaScope;
import com.chj.aigc.tenantservice.billing.QuotaScopeType;
import com.chj.aigc.tenantservice.billing.TenantBillingService;
import com.chj.aigc.tenantservice.tenant.TenantProject;
import com.chj.aigc.tenantservice.tenant.TenantWorkspaceService;
import com.chj.aigc.tenantservice.web.dto.CreateProjectRequest;
import com.chj.aigc.tenantservice.web.dto.CreateTenantMemberRequest;
import com.chj.aigc.tenantservice.web.dto.UpdateTenantMemberRoleRequest;
import com.chj.aigc.tenantservice.web.dto.UpdateTenantMemberStatusRequest;
import com.chj.aigc.tenantservice.web.dto.UpsertQuotaRequest;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * 租户工作台核心接口。
 */
@RestController
@RequestMapping("/api/tenant")
public class TenantApiController {
    private final TenantWorkspaceService tenantWorkspaceService;
    private final TenantBillingService tenantBillingService;
    private final AuthService authService;

    public TenantApiController(
            TenantWorkspaceService tenantWorkspaceService,
            TenantBillingService tenantBillingService,
            AuthService authService
    ) {
        this.tenantWorkspaceService = tenantWorkspaceService;
        this.tenantBillingService = tenantBillingService;
        this.authService = authService;
    }

    @GetMapping("/projects")
    public ApiResponse<List<TenantProject>> projects(HttpServletRequest request) {
        return ApiResponse.success(tenantWorkspaceService.projects(resolveTenantId(request)));
    }

    @PostMapping("/projects")
    public ApiResponse<TenantProject> createProject(@RequestBody CreateProjectRequest request, HttpServletRequest httpRequest) {
        requireTenantOwner(httpRequest);
        return ApiResponse.success(tenantWorkspaceService.createProject(request.projectId(), resolveTenantId(httpRequest), request.name()));
    }

    @GetMapping("/members")
    public ApiResponse<List<Map<String, Object>>> members(HttpServletRequest request) {
        return ApiResponse.success(authService.listTenantUsers(resolveTenantId(request)).stream().map(this::serializeUser).toList());
    }

    @PostMapping("/members")
    public ApiResponse<Map<String, Object>> createMember(@RequestBody CreateTenantMemberRequest request, HttpServletRequest httpRequest) {
        requireTenantOwner(httpRequest);
        AuthUser user = authService.createTenantUser(
                request.userId(),
                request.username(),
                request.password(),
                request.displayName(),
                request.roleKey(),
                resolveTenantId(httpRequest)
        );
        return ApiResponse.success(serializeUser(user));
    }

    @PostMapping("/members/{userId}/status")
    public ApiResponse<Map<String, Object>> updateMemberStatus(
            @PathVariable String userId,
            @RequestBody UpdateTenantMemberStatusRequest request,
            HttpServletRequest httpRequest
    ) {
        requireTenantOwner(httpRequest);
        AuthSession session = currentSession(httpRequest);
        if (session.userId().equals(userId) && !request.active()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不能停用当前登录的租户负责人");
        }
        return ApiResponse.success(serializeUser(authService.updateTenantUserStatus(userId, resolveTenantId(httpRequest), request.active())));
    }

    @PostMapping("/members/{userId}/role")
    public ApiResponse<Map<String, Object>> updateMemberRole(
            @PathVariable String userId,
            @RequestBody UpdateTenantMemberRoleRequest request,
            HttpServletRequest httpRequest
    ) {
        requireTenantOwner(httpRequest);
        AuthSession session = currentSession(httpRequest);
        if (session.userId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不能修改当前登录账号的角色");
        }
        return ApiResponse.success(serializeUser(authService.updateTenantUserRole(userId, resolveTenantId(httpRequest), request.roleKey())));
    }

    @GetMapping("/quotas")
    public ApiResponse<Map<String, Object>> quotas(HttpServletRequest request) {
        return ApiResponse.success(tenantBillingService.quotaSnapshot(resolveTenantId(request)));
    }

    @GetMapping("/quota-allocations")
    public ApiResponse<List<Map<String, Object>>> quotaAllocations(HttpServletRequest request) {
        return ApiResponse.success(tenantBillingService.listQuotaAllocations(resolveTenantId(request)).stream()
                .map(item -> Map.<String, Object>of(
                        "id", item.id(),
                        "scopeType", item.scope().scopeType().name(),
                        "scopeId", item.scope().scopeId(),
                        "dimension", item.dimension().name(),
                        "limit", item.limit(),
                        "used", item.used()
                ))
                .toList());
    }

    @PostMapping("/quotas")
    public ApiResponse<Map<String, Object>> upsertQuota(@RequestBody UpsertQuotaRequest request, HttpServletRequest httpRequest) {
        requireTenantOwner(httpRequest);
        return ApiResponse.success(tenantBillingService.upsertQuota(new QuotaAllocation(
                request.allocationId(),
                resolveTenantId(httpRequest),
                new QuotaScope(QuotaScopeType.valueOf(request.scopeType().toUpperCase()), request.scopeId()),
                QuotaDimension.valueOf(request.dimension().toUpperCase()),
                new BigDecimal(request.limit()),
                new BigDecimal(request.used())
        )));
    }

    private void requireTenantOwner(HttpServletRequest request) {
        if (!"tenant_owner".equals(currentSession(request).roleKey())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前账号没有执行该租户操作的权限");
        }
    }

    private AuthSession currentSession(HttpServletRequest request) {
        return (AuthSession) request.getAttribute(AuthInterceptor.REQUEST_SESSION_KEY);
    }

    private String resolveTenantId(HttpServletRequest request) {
        AuthSession session = currentSession(request);
        return session != null && session.tenantId() != null ? session.tenantId() : "tenant-demo";
    }

    private Map<String, Object> serializeUser(AuthUser user) {
        return Map.of(
                "id", user.id(),
                "username", user.username(),
                "displayName", user.displayName(),
                "roleKey", user.roleKey(),
                "tenantId", user.tenantId(),
                "active", user.active()
        );
    }
}
