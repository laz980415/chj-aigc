package com.chj.aigc.billing;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

public final class TenantBillingService {
    private final TenantBillingStore store;
    private final TenantFinanceService financeService = new TenantFinanceService();

    public TenantBillingService(TenantBillingStore store) {
        this.store = store;
        seedIfNeeded("tenant-demo");
    }

    public Map<String, Object> walletSnapshot(String tenantId) {
        TenantWallet wallet = wallet(tenantId);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tenantId", tenantId);
        payload.put("balance", wallet.balance().amount().toPlainString());
        payload.put("ledgerCount", wallet.ledgerEntries().size());
        return payload;
    }

    public Map<String, Object> recharge(String tenantId, String entryId, Money amount, String description, String referenceId) {
        TenantWallet wallet = wallet(tenantId);
        WalletLedgerEntry entry = financeService.recharge(wallet, entryId, amount, description, referenceId);
        store.saveLedgerEntry(entry);
        return walletSnapshot(tenantId);
    }

    public Map<String, Object> quotaSnapshot(String tenantId) {
        TenantQuotaBook quotaBook = quotaBook(tenantId);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("projectImageRemaining", quotaBook.remaining(new QuotaScope(QuotaScopeType.PROJECT, "project-demo"), QuotaDimension.IMAGE_COUNT));
        payload.put("userTokenRemaining", quotaBook.remaining(new QuotaScope(QuotaScopeType.USER, "user-demo"), QuotaDimension.TOKENS));
        return payload;
    }

    public Map<String, Object> upsertQuota(QuotaAllocation allocation) {
        store.saveQuotaAllocation(allocation);
        return quotaSnapshot(allocation.tenantId());
    }

    private TenantWallet wallet(String tenantId) {
        TenantWallet wallet = new TenantWallet(tenantId);
        store.listLedgerEntries(tenantId).forEach(wallet::append);
        return wallet;
    }

    private TenantQuotaBook quotaBook(String tenantId) {
        TenantQuotaBook quotaBook = new TenantQuotaBook(tenantId);
        store.listQuotaAllocations(tenantId).forEach(quotaBook::upsert);
        return quotaBook;
    }

    private void seedIfNeeded(String tenantId) {
        if (store.listLedgerEntries(tenantId).isEmpty()) {
            store.saveLedgerEntry(new WalletLedgerEntry(
                    "recharge-seed-1",
                    tenantId,
                    LedgerEntryType.RECHARGE,
                    Money.of("1000"),
                    "seed balance",
                    "seed",
                    java.time.Instant.now()
            ));
        }

        if (store.listQuotaAllocations(tenantId).isEmpty()) {
            store.saveQuotaAllocation(new QuotaAllocation(
                    "quota-project-1",
                    tenantId,
                    new QuotaScope(QuotaScopeType.PROJECT, "project-demo"),
                    QuotaDimension.IMAGE_COUNT,
                    new BigDecimal("20"),
                    BigDecimal.ZERO
            ));
            store.saveQuotaAllocation(new QuotaAllocation(
                    "quota-user-1",
                    tenantId,
                    new QuotaScope(QuotaScopeType.USER, "user-demo"),
                    QuotaDimension.TOKENS,
                    new BigDecimal("50000"),
                    new BigDecimal("1200")
            ));
        }
    }
}
