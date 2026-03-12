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
import com.chj.aigc.web.dto.CreateBrandRequest;
import com.chj.aigc.web.dto.CreateClientRequest;
import com.chj.aigc.web.dto.CreateTenantMemberRequest;
import com.chj.aigc.web.dto.RechargeRequest;
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
    public Map<String, Object> wallet() {
        return tenantBillingService.walletSnapshot("tenant-demo");
    }

    @PostMapping("/wallet/recharge")
    public Map<String, Object> recharge(@RequestBody RechargeRequest request, HttpServletRequest httpRequest) {
        requireAnyRole(httpRequest, "platform_super_admin", "tenant_owner");
        return tenantBillingService.recharge(
                "tenant-demo",
                request.entryId(),
                Money.of(request.amount()),
                request.description(),
                request.referenceId()
        );
    }

    @GetMapping("/quotas")
    public Map<String, Object> quotas() {
        return tenantBillingService.quotaSnapshot("tenant-demo");
    }

    @GetMapping("/quota-allocations")
    public List<Map<String, Object>> quotaAllocations() {
        return tenantBillingService.listQuotaAllocations("tenant-demo").stream()
                .map(allocation -> Map.<String, Object>of(
                        "id", allocation.id(),
                        "scopeType", allocation.scope().scopeType().name(),
                        "scopeId", allocation.scope().scopeId(),
                        "dimension", allocation.dimension().name(),
                        "limit", allocation.limit(),
                        "used", allocation.used()
                ))
                .toList();
    }

    @PostMapping("/quotas")
    public Map<String, Object> upsertQuota(@RequestBody UpsertQuotaRequest request, HttpServletRequest httpRequest) {
        requireAnyRole(httpRequest, "tenant_owner");
        return tenantBillingService.upsertQuota(new QuotaAllocation(
                request.allocationId(),
                "tenant-demo",
                new QuotaScope(
                        QuotaScopeType.valueOf(request.scopeType().toUpperCase()),
                        request.scopeId()
                ),
                QuotaDimension.valueOf(request.dimension().toUpperCase()),
                new BigDecimal(request.limit()),
                new BigDecimal(request.used())
        ));
    }

    @GetMapping("/clients")
    public List<Client> clients() {
        return tenantAssetCatalogService.clients("tenant-demo");
    }

    @GetMapping("/projects")
    public List<TenantProject> projects() {
        return tenantWorkspaceService.projects("tenant-demo");
    }

    @PostMapping("/projects")
    public TenantProject createProject(@RequestBody CreateProjectRequest request, HttpServletRequest httpRequest) {
        requireAnyRole(httpRequest, "tenant_owner");
        return tenantWorkspaceService.createProject(
                request.projectId(),
                "tenant-demo",
                request.name()
        );
    }

    @GetMapping("/members")
    public List<Map<String, Object>> members() {
        return tenantWorkspaceService.members("tenant-demo").stream()
                .map(this::serializeUser)
                .toList();
    }

    @PostMapping("/members")
    public Map<String, Object> createMember(@RequestBody CreateTenantMemberRequest request, HttpServletRequest httpRequest) {
        requireAnyRole(httpRequest, "tenant_owner");
        AuthUser user = authService.createTenantUser(
                request.userId(),
                request.username(),
                request.password(),
                request.displayName(),
                request.roleKey(),
                "tenant-demo"
        );
        return serializeUser(user);
    }

    @PostMapping("/clients")
    public Client createClient(@RequestBody CreateClientRequest request, HttpServletRequest httpRequest) {
        requireAnyRole(httpRequest, "tenant_owner");
        return tenantAssetCatalogService.createClient(
                request.clientId(),
                "tenant-demo",
                request.name()
        );
    }

    @GetMapping("/brands/{clientId}")
    public List<Brand> brands(@PathVariable String clientId) {
        return tenantAssetCatalogService.brands("tenant-demo", clientId);
    }

    @GetMapping("/brands")
    public List<Brand> allBrands() {
        return tenantAssetCatalogService.brands("tenant-demo", null);
    }

    @PostMapping("/brands")
    public Brand createBrand(@RequestBody CreateBrandRequest request, HttpServletRequest httpRequest) {
        requireAnyRole(httpRequest, "tenant_owner");
        return tenantAssetCatalogService.createBrand(
                request.brandId(),
                "tenant-demo",
                request.clientId(),
                request.name(),
                request.summary()
        );
    }

    @GetMapping("/assets")
    public List<Asset> assets() {
        return tenantAssetCatalogService.assets("tenant-demo");
    }

    private void requireAnyRole(HttpServletRequest request, String... roleKeys) {
        AuthSession session = (AuthSession) request.getAttribute(AuthInterceptor.REQUEST_SESSION_KEY);
        if (session == null || !Set.of(roleKeys).contains(session.roleKey())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前账号没有执行该租户操作的权限");
        }
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
