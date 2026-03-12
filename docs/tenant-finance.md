# Tenant Finance Domain

## Scope

This document defines the Java-side tenant finance model for V1.

It covers:
- tenant wallet balance
- recharge records
- usage deductions
- project quota allocation
- user quota allocation

## Wallet Rules

- Each tenant has a wallet ledger.
- Recharge creates positive immutable ledger entries.
- Usage creates negative immutable ledger entries.
- Current balance is derived from the ledger, not stored as a mutable single number only.

## Quota Rules

Quota allocations are scoped to either:
- `USER`
- `PROJECT`

Supported V1 dimensions:
- `TOKENS`
- `IMAGE_COUNT`
- `VIDEO_SECONDS`
- `DAILY_REQUESTS`
- `CONCURRENT_TASKS`

## Why This Lives In Java

- Wallets, quotas, and settlement are core business state.
- These rules must stay close to tenant, project, and permission models.
- Python workers should consume quota decisions from Java services instead of owning quota state themselves.
