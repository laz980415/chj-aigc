package com.chj.aigc.tenantservice.web;

import com.chj.aigc.tenantservice.asset.Asset;
import com.chj.aigc.tenantservice.asset.Brand;
import com.chj.aigc.tenantservice.asset.Client;
import com.chj.aigc.tenantservice.asset.TenantAssetCatalogService;
import com.chj.aigc.tenantservice.auth.AuthInterceptor;
import com.chj.aigc.tenantservice.auth.AuthService;
import com.chj.aigc.tenantservice.auth.AuthSession;
import com.chj.aigc.tenantservice.auth.AuthUser;
import com.chj.aigc.tenantservice.billing.QuotaAllocation;
import com.chj.aigc.tenantservice.billing.QuotaDimension;
import com.chj.aigc.tenantservice.billing.Money;
import com.chj.aigc.tenantservice.billing.QuotaScope;
import com.chj.aigc.tenantservice.billing.QuotaScopeType;
import com.chj.aigc.tenantservice.billing.TenantBillingService;
import com.chj.aigc.tenantservice.generation.GenerationJob;
import com.chj.aigc.tenantservice.generation.GenerationService;
import com.chj.aigc.tenantservice.tenant.TenantProject;
import com.chj.aigc.tenantservice.tenant.TenantWorkspaceService;
import com.chj.aigc.tenantservice.web.dto.CreateProjectRequest;
import com.chj.aigc.tenantservice.web.dto.CreateBrandRequest;
import com.chj.aigc.tenantservice.web.dto.CreateClientRequest;
import com.chj.aigc.tenantservice.web.dto.CreateGenerationJobRequest;
import com.chj.aigc.tenantservice.web.dto.CreatePaymentOrderRequest;
import com.chj.aigc.tenantservice.web.dto.CreateTenantMemberRequest;
import com.chj.aigc.tenantservice.web.dto.UpdateTenantMemberRoleRequest;
import com.chj.aigc.tenantservice.web.dto.UpdateTenantMemberStatusRequest;
import com.chj.aigc.tenantservice.web.dto.UpsertQuotaRequest;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
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
    private final TenantAssetCatalogService tenantAssetCatalogService;
    private final GenerationService generationService;

    public TenantApiController(
            TenantWorkspaceService tenantWorkspaceService,
            TenantBillingService tenantBillingService,
            AuthService authService,
            TenantAssetCatalogService tenantAssetCatalogService,
            GenerationService generationService
    ) {
        this.tenantWorkspaceService = tenantWorkspaceService;
        this.tenantBillingService = tenantBillingService;
        this.authService = authService;
        this.tenantAssetCatalogService = tenantAssetCatalogService;
        this.generationService = generationService;
    }

    @GetMapping("/projects")
    public ApiResponse<List<TenantProject>> projects(HttpServletRequest request) {
        return ApiResponse.success(tenantWorkspaceService.projects(resolveTenantId(request)));
    }

    @GetMapping("/wallet")
    public ApiResponse<Map<String, Object>> wallet(HttpServletRequest request) {
        return ApiResponse.success(tenantBillingService.walletSnapshot(resolveTenantId(request)));
    }

    @GetMapping("/wallet/payment-orders")
    public ApiResponse<List<Map<String, Object>>> paymentOrders(HttpServletRequest request) {
        return ApiResponse.success(tenantBillingService.paymentOrders(resolveTenantId(request)));
    }

    @PostMapping("/wallet/payment-orders/wechat")
    public ApiResponse<Map<String, Object>> createWeChatPaymentOrder(
            @RequestBody CreatePaymentOrderRequest request,
            HttpServletRequest httpRequest
    ) {
        requireTenantOwner(httpRequest);
        return ApiResponse.success(tenantBillingService.createMockWeChatPaymentOrder(
                resolveTenantId(httpRequest),
                request.orderId(),
                Money.of(request.amount()),
                request.description(),
                request.referenceId()
        ));
    }

    @PostMapping("/wallet/payment-orders/{orderId}/mock-paid")
    public ApiResponse<Map<String, Object>> mockPayOrder(@PathVariable String orderId, HttpServletRequest httpRequest) {
        requireTenantOwner(httpRequest);
        return ApiResponse.success(tenantBillingService.markPaymentOrderPaid(orderId));
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

    @GetMapping("/clients")
    public ApiResponse<List<Client>> clients(HttpServletRequest request) {
        return ApiResponse.success(tenantAssetCatalogService.clients(resolveTenantId(request)));
    }

    @PostMapping("/clients")
    public ApiResponse<Client> createClient(@RequestBody CreateClientRequest request, HttpServletRequest httpRequest) {
        requireTenantOwner(httpRequest);
        return ApiResponse.success(tenantAssetCatalogService.createClient(
                request.clientId(),
                resolveTenantId(httpRequest),
                request.name()
        ));
    }

    @GetMapping("/brands")
    public ApiResponse<List<Brand>> brands(HttpServletRequest request) {
        return ApiResponse.success(tenantAssetCatalogService.brands(resolveTenantId(request), null));
    }

    @GetMapping("/brands/{clientId}")
    public ApiResponse<List<Brand>> brandsByClient(@PathVariable String clientId, HttpServletRequest request) {
        return ApiResponse.success(tenantAssetCatalogService.brands(resolveTenantId(request), clientId));
    }

    @PostMapping("/brands")
    public ApiResponse<Brand> createBrand(@RequestBody CreateBrandRequest request, HttpServletRequest httpRequest) {
        requireTenantOwner(httpRequest);
        return ApiResponse.success(tenantAssetCatalogService.createBrand(
                request.brandId(),
                resolveTenantId(httpRequest),
                request.clientId(),
                request.name(),
                request.summary()
        ));
    }

    @GetMapping("/assets")
    public ApiResponse<List<Asset>> assets(HttpServletRequest request) {
        return ApiResponse.success(tenantAssetCatalogService.assets(resolveTenantId(request)));
    }

    @GetMapping("/generation/jobs")
    public ApiResponse<List<Map<String, Object>>> generationJobs(HttpServletRequest request) {
        return ApiResponse.success(generationService.listJobs(resolveTenantId(request)).stream()
                .map(this::serializeGenerationJob)
                .toList());
    }

    @PostMapping("/generation/jobs")
    public ApiResponse<Map<String, Object>> submitGeneration(
            @RequestBody CreateGenerationJobRequest request,
            HttpServletRequest httpRequest
    ) {
        GenerationJob job = generationService.submit(
                resolveTenantId(httpRequest),
                currentSession(httpRequest),
                new GenerationService.SubmitGenerationCommand(
                        request.projectId(),
                        request.modelAlias(),
                        request.capability(),
                        request.userPrompt(),
                        request.brandId(),
                        request.brandName(),
                        request.brandSummary()
                )
        );
        return ApiResponse.success(serializeGenerationJob(job));
    }

    @GetMapping("/generation/jobs/{jobId}")
    public ApiResponse<Map<String, Object>> generationJob(@PathVariable String jobId, HttpServletRequest request) {
        return ApiResponse.success(serializeGenerationJob(generationService.refresh(resolveTenantId(request), jobId)));
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

    private Map<String, Object> serializeGenerationJob(GenerationJob job) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("job_id", job.id());
        payload.put("project_id", job.projectId());
        payload.put("model_alias", job.modelAlias());
        payload.put("capability", job.capability().value());
        payload.put("brand_id", job.brandId() == null ? "" : job.brandId());
        payload.put("brand_name", job.brandName());
        payload.put("status", job.status().value());
        payload.put("output_text", job.outputText());
        payload.put("output_uri", job.outputUri());
        payload.put("error_message", job.errorMessage());
        payload.put("provider_id", job.providerId());
        payload.put("provider_model_name", job.providerModelName());
        payload.put("provider_job_id", job.providerJobId());
        payload.put("input_tokens", job.inputTokens());
        payload.put("output_tokens", job.outputTokens());
        payload.put("image_count", job.imageCount());
        payload.put("video_seconds", job.videoSeconds());
        payload.put("charge_amount", job.chargeAmount() == null ? null : job.chargeAmount().toPlainString());
        payload.put("settled", job.settled());
        payload.put("created_at", job.createdAt());
        payload.put("updated_at", job.updatedAt());
        return payload;
    }
}
