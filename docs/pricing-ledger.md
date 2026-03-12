# Pricing And Ledger Model

## Goal

Create an internal pricing layer so tenant billing stays stable even if vendor models or vendor prices change.

## Design

### Metering Dimensions

The billing system reuses platform metering dimensions:

- `input_tokens`
- `output_tokens`
- `image_count`
- `video_seconds`

### Price Rule

A price rule is attached to a `platform_model_id` and `capability_key`.

Each rule contains one or more price components:
- dimension
- unit price
- unit size

Examples:
- text input billed per 1000 tokens
- text output billed per 1000 tokens
- images billed per generated image
- video billed per generated second

### Price Unit

The platform can charge in:
- `credits`
- `currency`

V1 should use internal `credits` for tenant-facing balance management.

### Ledger

Ledger entries are immutable records with:
- tenant id
- amount
- price unit
- reference id
- reference type
- platform model id
- vendor reference snapshot
- charge breakdown snapshot

This preserves history even if:
- platform price rules change later
- a platform model alias switches vendors
- vendor raw costs change over time

## Billing Flow

1. Generation usage is measured in platform dimensions.
2. Pricing selects the active rule for the platform model alias.
3. A charge quote is computed from the usage payload.
4. Ledger stores an immutable usage entry with the exact breakdown.
5. Tenant balance is derived from recharge and usage entries.

## Why Snapshot Vendor References

The ledger stores both:
- platform-facing model id
- vendor-facing provider and model name

That gives:
- stable tenant billing
- vendor-cost auditing
- traceability during future supplier replacements
