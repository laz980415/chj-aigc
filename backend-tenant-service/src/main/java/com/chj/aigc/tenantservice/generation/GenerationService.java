package com.chj.aigc.tenantservice.generation;

import com.chj.aigc.tenantservice.asset.Asset;
import com.chj.aigc.tenantservice.asset.Brand;
import com.chj.aigc.tenantservice.asset.Client;
import com.chj.aigc.tenantservice.asset.TenantAssetCatalogService;
import com.chj.aigc.tenantservice.auth.AuthSession;
import com.chj.aigc.tenantservice.billing.TenantBillingService;
import com.chj.aigc.tenantservice.tenant.TenantProject;
import com.chj.aigc.tenantservice.tenant.TenantWorkspaceService;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * 生成任务业务编排。
 */
public final class GenerationService {
    private final GenerationStore store;
    private final TenantWorkspaceService tenantWorkspaceService;
    private final TenantAssetCatalogService tenantAssetCatalogService;
    private final TenantBillingService tenantBillingService;
    private final ModelAccessClient modelAccessClient;
    private final ModelGatewayClient modelGatewayClient;

    public GenerationService(
            GenerationStore store,
            TenantWorkspaceService tenantWorkspaceService,
            TenantAssetCatalogService tenantAssetCatalogService,
            TenantBillingService tenantBillingService,
            ModelAccessClient modelAccessClient,
            ModelGatewayClient modelGatewayClient
    ) {
        this.store = store;
        this.tenantWorkspaceService = tenantWorkspaceService;
        this.tenantAssetCatalogService = tenantAssetCatalogService;
        this.tenantBillingService = tenantBillingService;
        this.modelAccessClient = modelAccessClient;
        this.modelGatewayClient = modelGatewayClient;
    }

    public List<GenerationJob> listJobs(String tenantId) {
        return store.listJobs(tenantId).stream()
                .sorted(Comparator.comparing(GenerationJob::createdAt).reversed())
                .toList();
    }

    public GenerationJob submit(String tenantId, AuthSession session, SubmitGenerationCommand command) {
        String projectId = resolveProjectId(tenantId, command.projectId());
        ResolvedBrandContext brandContext = resolveBrandContext(
                tenantId,
                projectId,
                command.brandId(),
                command.brandName(),
                command.brandSummary()
        );

        ModelAccessClient.ModelAccessDecision decision = modelAccessClient.evaluate(
                tenantId,
                projectId,
                Set.of(session.roleKey()),
                command.modelAlias()
        );
        if (!decision.allowed()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "模型访问被拒绝: " + decision.reason());
        }

        ModelGatewayClient.JobResult remote = modelGatewayClient.submitJob(new ModelGatewayClient.SubmitPayload(
                tenantId,
                projectId,
                session.userId(),
                command.modelAlias(),
                command.capability(),
                command.userPrompt(),
                brandContext.clientName(),
                brandContext.brandName(),
                brandContext.brandSummary(),
                brandContext.assets()
        ));

        Instant now = Instant.now();
        GenerationJob job = new GenerationJob(
                remote.jobId(),
                tenantId,
                projectId,
                session.userId(),
                session.roleKey(),
                command.modelAlias(),
                GenerationCapability.fromValue(command.capability()),
                brandContext.brandId(),
                brandContext.brandName(),
                brandContext.brandSummary(),
                brandContext.clientName(),
                command.userPrompt(),
                GenerationJobStatus.fromValue(remote.status()),
                remote.outputText(),
                remote.outputUri(),
                remote.errorMessage(),
                remote.providerId(),
                remote.providerModelName(),
                remote.providerJobId(),
                remote.inputTokens(),
                remote.outputTokens(),
                remote.imageCount(),
                remote.videoSeconds(),
                null,
                false,
                now,
                now
        );
        job = settleIfNeeded(job);
        store.saveJob(job);
        return job;
    }

    public GenerationJob refresh(String tenantId, String jobId) {
        GenerationJob existing = store.findJob(jobId)
                .filter(job -> tenantId.equals(job.tenantId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "生成任务不存在"));
        if (existing.status() == GenerationJobStatus.SUCCEEDED || existing.status() == GenerationJobStatus.FAILED) {
            return existing;
        }

        ModelGatewayClient.JobResult remote = modelGatewayClient.fetchJob(jobId);
        GenerationJob updated = new GenerationJob(
                existing.id(),
                existing.tenantId(),
                existing.projectId(),
                existing.actorId(),
                existing.roleKey(),
                existing.modelAlias(),
                existing.capability(),
                existing.brandId(),
                existing.brandName(),
                existing.brandSummary(),
                existing.clientName(),
                existing.userPrompt(),
                GenerationJobStatus.fromValue(remote.status()),
                remote.outputText(),
                remote.outputUri(),
                remote.errorMessage(),
                remote.providerId(),
                remote.providerModelName(),
                remote.providerJobId(),
                remote.inputTokens(),
                remote.outputTokens(),
                remote.imageCount(),
                remote.videoSeconds(),
                existing.chargeAmount(),
                existing.settled(),
                existing.createdAt(),
                Instant.now()
        );
        updated = settleIfNeeded(updated);
        store.saveJob(updated);
        return updated;
    }

    private GenerationJob settleIfNeeded(GenerationJob job) {
        if (job.settled() || job.status() != GenerationJobStatus.SUCCEEDED) {
            return job;
        }
        TenantBillingService.GenerationSettlement settlement = tenantBillingService.settleGeneration(
                job.id(),
                job.tenantId(),
                job.projectId(),
                job.actorId(),
                job.capability().value(),
                job.inputTokens(),
                job.outputTokens(),
                job.imageCount(),
                job.videoSeconds()
        );
        return new GenerationJob(
                job.id(),
                job.tenantId(),
                job.projectId(),
                job.actorId(),
                job.roleKey(),
                job.modelAlias(),
                job.capability(),
                job.brandId(),
                job.brandName(),
                job.brandSummary(),
                job.clientName(),
                job.userPrompt(),
                job.status(),
                job.outputText(),
                job.outputUri(),
                job.errorMessage(),
                job.providerId(),
                job.providerModelName(),
                job.providerJobId(),
                settlement.inputTokens(),
                settlement.outputTokens(),
                settlement.imageCount(),
                settlement.videoSeconds(),
                settlement.chargeAmount(),
                true,
                job.createdAt(),
                Instant.now()
        );
    }

    private String resolveProjectId(String tenantId, String projectId) {
        List<TenantProject> projects = tenantWorkspaceService.projects(tenantId);
        if (projectId != null && !projectId.isBlank()) {
            boolean exists = projects.stream().map(TenantProject::id).anyMatch(projectId::equals);
            if (!exists) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "项目不存在: " + projectId);
            }
            return projectId;
        }
        return projects.stream().findFirst().map(TenantProject::id).orElse("project-demo");
    }

    private ResolvedBrandContext resolveBrandContext(
            String tenantId,
            String projectId,
            String brandId,
            String brandName,
            String brandSummary
    ) {
        List<Brand> brands = tenantAssetCatalogService.brands(tenantId, null);
        Brand brand = null;
        if (brandId != null && !brandId.isBlank()) {
            brand = brands.stream().filter(item -> brandId.equals(item.id())).findFirst().orElse(null);
        }
        if (brand == null && brandName != null && !brandName.isBlank()) {
            brand = brands.stream().filter(item -> brandName.equals(item.name())).findFirst().orElse(null);
        }

        List<Client> clients = tenantAssetCatalogService.clients(tenantId);
        String clientName = Optional.ofNullable(brand)
                .flatMap(item -> clients.stream().filter(client -> client.id().equals(item.clientId())).findFirst())
                .map(Client::name)
                .orElse("默认客户");
        String finalBrandName = brand != null ? brand.name() : (brandName == null || brandName.isBlank() ? "默认品牌" : brandName);
        String finalBrandSummary = brand != null ? brand.summary() : (brandSummary == null ? "" : brandSummary);
        String finalBrandId = brand != null ? brand.id() : null;
        Brand selectedBrand = brand;
        List<ModelGatewayClient.AssetPayload> assets = tenantAssetCatalogService.assets(tenantId).stream()
                .filter(asset -> selectedBrand == null || asset.brandId().equals(selectedBrand.id()))
                .filter(asset -> asset.projectId().equals(projectId))
                .limit(5)
                .map(this::toAssetPayload)
                .toList();

        return new ResolvedBrandContext(finalBrandId, finalBrandName, finalBrandSummary, clientName, assets);
    }

    private ModelGatewayClient.AssetPayload toAssetPayload(Asset asset) {
        return new ModelGatewayClient.AssetPayload(
                asset.id(),
                asset.name(),
                asset.kind().name().toLowerCase(),
                asset.uri(),
                asset.tags().stream().sorted().toList(),
                ""
        );
    }

    public record SubmitGenerationCommand(
            String projectId,
            String modelAlias,
            String capability,
            String userPrompt,
            String brandId,
            String brandName,
            String brandSummary
    ) {
    }

    private record ResolvedBrandContext(
            String brandId,
            String brandName,
            String brandSummary,
            String clientName,
            List<ModelGatewayClient.AssetPayload> assets
    ) {
    }
}
