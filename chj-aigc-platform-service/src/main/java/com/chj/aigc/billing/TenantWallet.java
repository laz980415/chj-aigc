package com.chj.aigc.billing;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class TenantWallet {
    private final String tenantId;
    private final List<WalletLedgerEntry> ledgerEntries = new ArrayList<>();

    public TenantWallet(String tenantId) {
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
    }

    public String tenantId() {
        return tenantId;
    }

    public void append(WalletLedgerEntry entry) {
        if (!tenantId.equals(entry.tenantId())) {
            throw new IllegalArgumentException("Ledger entry tenant mismatch");
        }
        ledgerEntries.add(entry);
    }

    public Money balance() {
        Money balance = Money.of("0");
        for (WalletLedgerEntry entry : ledgerEntries) {
            balance = balance.add(entry.amount());
        }
        return balance;
    }

    public List<WalletLedgerEntry> ledgerEntries() {
        return List.copyOf(ledgerEntries);
    }
}
