package com.chj.aigc.web;

import com.chj.aigc.asset.Asset;
import com.chj.aigc.asset.Brand;
import com.chj.aigc.asset.Client;
import com.chj.aigc.asset.TenantAssetCatalogService;
import com.chj.aigc.auth.AuthInterceptor;
import com.chj.aigc.auth.AuthSession;
import com.chj.aigc.auth.AuthService;
import com.chj.aigc.auth.AuthUser;
import com.chj.aigc.billing.Money;
import com.chj.aigc.billing.QuotaAllocation;
import com.chj.aigc.billing.QuotaDimension;
import com.chj.aigc.billing.QuotaScope;
import com.chj.aigc.billing.QuotaScopeType;
import com.chj.aigc.billing.TenantBillingService;
import com.chj.aigc.tenant.TenantProject;
import com.chj.aigc.tenant.TenantWorkspaceService;
import com.chj.aigc.web.dto.CreateProjectRequest;
import com.chj.aigc.web.dto.CreatePaymentOrderRequest;
import com.chj.aigc.web.dto.CreateBrandRequest;
import com.chj.aigc.web.dto.CreateClientRequest;
import com.chj.aigc.web.dto.CreateTenantMemberRequest;
import com.chj.aigc.web.dto.RechargeRequest;
import com.chj.aigc.web.dto.UpdateTenantMemberRoleRequest;
import com.chj.aigc.web.dto.UpdateTenantMemberStatusRequest;
import com.chj.aigc.web.dto.UpsertQuotaRequest;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

/**
 * 租户工作台 API。
 * 负责租户负责人和租户成员访问的钱包、额度、项目、成员、客户、品牌和素材能力。
 */
@RestController
@RequestMapping("/api/tenant")
public class TenantApiController {
    private final TenantBillingService tenantBillingService;
    private final TenantAssetCatalogService tenantAssetCatalogService;
    private final TenantWorkspaceService tenantWorkspaceService;
    private final AuthService authService;

    public TenantApiController(
            TenantBillingService tenantBillingService,
            TenantAssetCatalogService tenantAssetCatalogService,
            TenantWorkspaceService tenantWorkspaceService,
            AuthService authService
    ) {
        this.tenantBillingService = tenantBillingService;
        this.tenantAssetCatalogService = tenantAssetCatalogService;
        this.tenantWorkspaceService = tenantWorkspaceService;
        this.authService = authService;
    }

    @GetMapping("/wallet")
    public ApiResponse<Map<String, Object>> wallet(HttpServletRequest httpRequest) {
        return ApiResponse.success(tenantBillingService.walletSnapshot(resolveTenantId(httpRequest)));
    }

    @PostMapping("/wallet/recharge")
    public ApiResponse<Map<String, Object>> recharge(@RequestBody RechargeRequest request, HttpServletRequest httpRequest) {
        requireAnyRole(httpRequest, "platform_super_admin", "tenant_owner");
        return ApiResponse.success(tenantBillingService.recharge(
                resolveRechargeTenantId(request, httpRequest),
                request.entryId(),
                Money.of(request.amount()),
                request.description(),
                request.referenceId()
        ));
    }

    @GetMapping("/wallet/payment-orders")
    public ApiResponse<List<Map<String, Object>>> paymentOrders(HttpServletRequest httpRequest) {
        return ApiResponse.success(tenantBillingService.paymentOrders(resolveTenantId(httpRequest)));
    }

    @PostMapping("/wallet/payment-orders/wechat")
    public ApiResponse<Map<String, Object>> createWeChatPaymentOrder(
            @RequestBody CreatePaymentOrderRequest request,
            HttpServletRequest httpRequest
    ) {
        requireAnyRole(httpRequest, "platform_super_admin", "tenant_owner");
        return ApiResponse.success(tenantBillingService.createMockWeChatPaymentOrder(
                resolveRechargeTenantId(new RechargeRequest(request.orderId(), request.tenantId(), request.amount(), request.description(), request.referenceId()), httpRequest),
                request.orderId(),
                Money.of(request.amount()),
                request.description(),
                request.referenceId()
        ));
    }

    @PostMapping("/wallet/payment-orders/{orderId}/mock-paid")
    public ApiResponse<Map<String, Object>> mockPayOrder(@PathVariable String orderId, HttpServletRequest httpRequest) {
        requireAnyRole(httpRequest, "platform_super_admin", "tenant_owner");
        return ApiResponse.success(tenantBillingService.markPaymentOrderPaid(orderId));
    }

    @GetMapping("/quotas")
    public ApiResponse<Map<String, Object>> quotas(HttpServletRequest httpRequest) {
        return ApiResponse.success(tenantBillingService.quotaSnapshot(resolveTenantId(httpRequest)));
    }

    @GetMapping("/quota-allocations")
    public ApiResponse<List<Map<String, Object>>> quotaAllocations(HttpServletRequest httpRequest) {
        return ApiResponse.success(tenantBillingService.listQuotaAllocations(resolveTenantId(httpRequest)).stream()
                .map(allocation -> Map.<String, Object>of(
                        "id", allocation.id(),
                        "scopeType", allocation.scope().scopeType().name(),
                        "scopeId", allocation.scope().scopeId(),
                        "dimension", allocation.dimension().name(),
                        "limit", allocation.limit(),
                        "used", allocation.used()
                ))
                .toList());
    }

    @PostMapping("/quotas")
    public ApiResponse<Map<String, Object>> upsertQuota(@RequestBody UpsertQuotaRequest request, HttpServletRequest httpRequest) {
        requireAnyRole(httpRequest, "tenant_owner");
        return ApiResponse.success(tenantBillingService.upsertQuota(new QuotaAllocation(
                request.allocationId(),
                resolveTenantId(httpRequest),
                new QuotaScope(
                        QuotaScopeType.valueOf(request.scopeType().toUpperCase()),
                        request.scopeId()
                ),
                QuotaDimension.valueOf(request.dimension().toUpperCase()),
                new BigDecimal(request.limit()),
                new BigDecimal(request.used())
        )));
    }

    @GetMapping("/clients")
    public ApiResponse<List<Client>> clients(HttpServletRequest httpRequest) {
        return ApiResponse.success(tenantAssetCatalogService.clients(resolveTenantId(httpRequest)));
    }

    @GetMapping("/projects")
    public ApiResponse<List<TenantProject>> projects(HttpServletRequest httpRequest) {
        return ApiResponse.success(tenantWorkspaceService.projects(resolveTenantId(httpRequest)));
    }

    @PostMapping("/projects")
    public ApiResponse<TenantProject> createProject(@RequestBody CreateProjectRequest request, HttpServletRequest httpRequest) {
        requireAnyRole(httpRequest, "tenant_owner");
        return ApiResponse.success(tenantWorkspaceService.createProject(
                request.projectId(),
                resolveTenantId(httpRequest),
                request.name()
        ));
    }

    @GetMapping("/members")
    public ApiResponse<List<Map<String, Object>>> members(HttpServletRequest httpRequest) {
        return ApiResponse.success(authService.listTenantUsers(resolveTenantId(httpRequest)).stream()
                .map(this::serializeUser)
                .toList());
    }

    @PostMapping("/members")
    public ApiResponse<Map<String, Object>> createMember(@RequestBody CreateTenantMemberRequest request, HttpServletRequest httpRequest) {
        requireAnyRole(httpRequest, "tenant_owner");
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
        requireAnyRole(httpRequest, "tenant_owner");
        AuthSession session = currentSession(httpRequest);
        if (session != null && session.userId().equals(userId) && !request.active()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不能停用当前登录的租户负责人");
        }
        AuthUser user = authService.updateTenantUserStatus(userId, resolveTenantId(httpRequest), request.active());
        return ApiResponse.success(serializeUser(user));
    }

    @PostMapping("/members/{userId}/role")
    public ApiResponse<Map<String, Object>> updateMemberRole(
            @PathVariable String userId,
            @RequestBody UpdateTenantMemberRoleRequest request,
            HttpServletRequest httpRequest
    ) {
        requireAnyRole(httpRequest, "tenant_owner");
        AuthSession session = currentSession(httpRequest);
        if (session != null && session.userId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不能修改当前登录账号的角色");
        }
        AuthUser user = authService.updateTenantUserRole(userId, resolveTenantId(httpRequest), request.roleKey());
        return ApiResponse.success(serializeUser(user));
    }

    @PostMapping("/clients")
    public ApiResponse<Client> createClient(@RequestBody CreateClientRequest request, HttpServletRequest httpRequest) {
        requireAnyRole(httpRequest, "tenant_owner");
        return ApiResponse.success(tenantAssetCatalogService.createClient(
                request.clientId(),
                resolveTenantId(httpRequest),
                request.name()
        ));
    }

    @GetMapping("/brands/{clientId}")
    public ApiResponse<List<Brand>> brands(@PathVariable String clientId, HttpServletRequest httpRequest) {
        return ApiResponse.success(tenantAssetCatalogService.brands(resolveTenantId(httpRequest), clientId));
    }

    @GetMapping("/brands")
    public ApiResponse<List<Brand>> allBrands(HttpServletRequest httpRequest) {
        return ApiResponse.success(tenantAssetCatalogService.brands(resolveTenantId(httpRequest), null));
    }

    @PostMapping("/brands")
    public ApiResponse<Brand> createBrand(@RequestBody CreateBrandRequest request, HttpServletRequest httpRequest) {
        requireAnyRole(httpRequest, "tenant_owner");
        return ApiResponse.success(tenantAssetCatalogService.createBrand(
                request.brandId(),
                resolveTenantId(httpRequest),
                request.clientId(),
                request.name(),
                request.summary()
        ));
    }

    @GetMapping("/assets")
    public ApiResponse<List<Asset>> assets(HttpServletRequest httpRequest) {
        return ApiResponse.success(tenantAssetCatalogService.assets(resolveTenantId(httpRequest)));
    }

    private void requireAnyRole(HttpServletRequest request, String... roleKeys) {
        AuthSession session = currentSession(request);
        if (session == null || !Set.of(roleKeys).contains(session.roleKey())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前账号没有执行该租户操作的权限");
        }
    }

    private AuthSession currentSession(HttpServletRequest request) {
        return (AuthSession) request.getAttribute(AuthInterceptor.REQUEST_SESSION_KEY);
    }

    private String resolveTenantId(HttpServletRequest request) {
        AuthSession session = currentSession(request);
        if (session != null && session.tenantId() != null && !session.tenantId().isBlank()) {
            return session.tenantId();
        }
        return "tenant-demo";
    }

    /**
     * 超管可显式指定目标租户，租户负责人仍固定作用于自己的租户。
     */
    private String resolveRechargeTenantId(RechargeRequest request, HttpServletRequest httpRequest) {
        AuthSession session = currentSession(httpRequest);
        if (session != null && "platform_super_admin".equals(session.roleKey())
                && request.tenantId() != null && !request.tenantId().isBlank()) {
            return request.tenantId();
        }
        return resolveTenantId(httpRequest);
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
