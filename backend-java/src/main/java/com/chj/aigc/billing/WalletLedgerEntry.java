package com.chj.aigc.billing;

import java.time.Instant;
import java.util.Objects;

public record WalletLedgerEntry(
        String id,
        String tenantId,
        LedgerEntryType type,
        Money amount,
        String description,
        String referenceId,
        Instant createdAt
) {
    public WalletLedgerEntry {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(referenceId, "referenceId");
        Objects.requireNonNull(createdAt, "createdAt");
    }
}
