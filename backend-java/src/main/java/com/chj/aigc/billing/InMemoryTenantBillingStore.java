package com.chj.aigc.billing;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class InMemoryTenantBillingStore implements TenantBillingStore {
    private final List<WalletLedgerEntry> ledgerEntries = new ArrayList<>();
    private final List<QuotaAllocation> quotaAllocations = new ArrayList<>();

    @Override
    public List<WalletLedgerEntry> listLedgerEntries(String tenantId) {
        return ledgerEntries.stream()
                .filter(entry -> entry.tenantId().equals(tenantId))
                .toList();
    }

    @Override
    public void saveLedgerEntry(WalletLedgerEntry entry) {
        ledgerEntries.add(Objects.requireNonNull(entry, "entry"));
    }

    @Override
    public List<QuotaAllocation> listQuotaAllocations(String tenantId) {
        return quotaAllocations.stream()
                .filter(allocation -> allocation.tenantId().equals(tenantId))
                .toList();
    }

    @Override
    public void saveQuotaAllocation(QuotaAllocation allocation) {
        quotaAllocations.removeIf(existing ->
                existing.tenantId().equals(allocation.tenantId())
                        && existing.scope().scopeType() == allocation.scope().scopeType()
                        && existing.scope().scopeId().equals(allocation.scope().scopeId())
                        && existing.dimension() == allocation.dimension()
        );
        quotaAllocations.add(Objects.requireNonNull(allocation, "allocation"));
    }
}
