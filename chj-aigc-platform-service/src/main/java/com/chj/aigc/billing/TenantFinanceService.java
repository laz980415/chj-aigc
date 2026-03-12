package com.chj.aigc.billing;

import java.time.Instant;
import java.util.Objects;

public final class TenantFinanceService {
    public WalletLedgerEntry recharge(
            TenantWallet wallet,
            String entryId,
            Money amount,
            String description,
            String referenceId
    ) {
        Objects.requireNonNull(wallet, "wallet");
        Objects.requireNonNull(amount, "amount");
        if (amount.isNegative()) {
            throw new IllegalArgumentException("recharge amount must be non-negative");
        }
        WalletLedgerEntry entry = new WalletLedgerEntry(
                entryId,
                wallet.tenantId(),
                LedgerEntryType.RECHARGE,
                amount,
                description,
                referenceId,
                Instant.now()
        );
        wallet.append(entry);
        return entry;
    }

    public WalletLedgerEntry deductUsage(
            TenantWallet wallet,
            String entryId,
            Money amount,
            String description,
            String referenceId
    ) {
        Objects.requireNonNull(wallet, "wallet");
        Objects.requireNonNull(amount, "amount");
        WalletLedgerEntry entry = new WalletLedgerEntry(
                entryId,
                wallet.tenantId(),
                LedgerEntryType.USAGE,
                new Money(amount.amount().negate()),
                description,
                referenceId,
                Instant.now()
        );
        wallet.append(entry);
        return entry;
    }
}
