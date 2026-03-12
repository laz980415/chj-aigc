package com.chj.aigc.web;

import com.chj.aigc.asset.Asset;
import com.chj.aigc.asset.AssetFilter;
import com.chj.aigc.asset.AssetKind;
import com.chj.aigc.asset.AssetLibraryService;
import com.chj.aigc.asset.Brand;
import com.chj.aigc.asset.BrandRule;
import com.chj.aigc.asset.BrandRuleKind;
import com.chj.aigc.asset.Client;
import com.chj.aigc.billing.Money;
import com.chj.aigc.billing.QuotaAllocation;
import com.chj.aigc.billing.QuotaDimension;
import com.chj.aigc.billing.QuotaScope;
import com.chj.aigc.billing.QuotaScopeType;
import com.chj.aigc.billing.TenantFinanceService;
import com.chj.aigc.billing.TenantQuotaBook;
import com.chj.aigc.billing.TenantWallet;
import com.chj.aigc.web.dto.RechargeRequest;
import com.chj.aigc.web.dto.UpsertQuotaRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tenant")
public class TenantApiController {
    private final TenantWallet wallet = new TenantWallet("tenant-demo");
    private final TenantQuotaBook quotaBook = new TenantQuotaBook("tenant-demo");
    private final TenantFinanceService financeService = new TenantFinanceService();
    private final AssetLibraryService assetLibraryService = new AssetLibraryService();

    public TenantApiController() {
        if (wallet.ledgerEntries().isEmpty()) {
            financeService.recharge(wallet, "recharge-seed-1", Money.of("1000"), "seed balance", "seed");
        }

        quotaBook.upsert(new QuotaAllocation(
                "quota-project-1",
                "tenant-demo",
                new QuotaScope(QuotaScopeType.PROJECT, "project-demo"),
                QuotaDimension.IMAGE_COUNT,
                new BigDecimal("20"),
                BigDecimal.ZERO
        ));
        quotaBook.upsert(new QuotaAllocation(
                "quota-user-1",
                "tenant-demo",
                new QuotaScope(QuotaScopeType.USER, "user-demo"),
                QuotaDimension.TOKENS,
                new BigDecimal("50000"),
                new BigDecimal("1200")
        ));

        assetLibraryService.addClient(new Client("client-demo", "tenant-demo", "Demo Advertiser", true));
        assetLibraryService.addBrand(new Brand(
                "brand-demo",
                "tenant-demo",
                "client-demo",
                "Demo Brand",
                "Modern skincare brand for urban users",
                true
        ));
        assetLibraryService.addBrandRule(new BrandRule(
                "brand-rule-1",
                "tenant-demo",
                "brand-demo",
                BrandRuleKind.FORBIDDEN_STATEMENT,
                "Do not claim medical efficacy",
                true
        ));
        assetLibraryService.addAsset(new Asset(
                "asset-demo-1",
                "tenant-demo",
                "project-demo",
                "client-demo",
                "brand-demo",
                "Hero packshot",
                AssetKind.IMAGE,
                "oss://demo-assets/hero-packshot.png",
                Set.of("hero", "product"),
                true
        ));
    }

    @GetMapping("/wallet")
    public Map<String, Object> wallet() {
        return Map.of(
                "tenantId", wallet.tenantId(),
                "balance", wallet.balance().amount().toPlainString(),
                "ledgerCount", wallet.ledgerEntries().size()
        );
    }

    @PostMapping("/wallet/recharge")
    public Map<String, Object> recharge(@RequestBody RechargeRequest request) {
        financeService.recharge(
                wallet,
                request.entryId(),
                Money.of(request.amount()),
                request.description(),
                request.referenceId()
        );
        return wallet();
    }

    @GetMapping("/quotas")
    public Map<String, Object> quotas() {
        return Map.of(
                "projectImageRemaining",
                quotaBook.remaining(new QuotaScope(QuotaScopeType.PROJECT, "project-demo"), QuotaDimension.IMAGE_COUNT),
                "userTokenRemaining",
                quotaBook.remaining(new QuotaScope(QuotaScopeType.USER, "user-demo"), QuotaDimension.TOKENS)
        );
    }

    @PostMapping("/quotas")
    public Map<String, Object> upsertQuota(@RequestBody UpsertQuotaRequest request) {
        quotaBook.upsert(new QuotaAllocation(
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
        return quotas();
    }

    @GetMapping("/clients")
    public List<Client> clients() {
        return assetLibraryService.clientsByTenant("tenant-demo");
    }

    @GetMapping("/brands/{clientId}")
    public List<Brand> brands(@PathVariable String clientId) {
        return assetLibraryService.brandsByClient("tenant-demo", clientId);
    }

    @GetMapping("/assets")
    public List<Asset> assets() {
        return assetLibraryService.findAssets(new AssetFilter(
                Optional.of("tenant-demo"),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Set.of()
        ));
    }
}
