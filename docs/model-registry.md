# Model Registry

## Goal

Expose stable platform model aliases to tenants while allowing the underlying vendor models to change over time.

## Core Concepts

### Provider

Represents a third-party model platform.

Fields:
- `id`
- `name`
- `api_base_url`
- `status`

### Platform Model

Represents the business-facing model exposed by the SaaS platform.

Fields:
- `id`
- `alias`
- `display_name`
- `capability`
- `metering_dimensions`
- `status`

Example aliases:
- `copy-standard`
- `image-standard`
- `video-standard`

### Provider Model Binding

Maps a platform model alias to a concrete provider model name.

Fields:
- `platform_model_id`
- `provider_id`
- `provider_model_name`
- `priority`
- `status`
- `supports_brand_grounding`
- `supports_async_jobs`

## Routing Rules

- Business services request a `platform model alias`.
- Registry resolves the alias into the highest-priority active provider binding.
- If a binding is disabled, the next available binding can be selected.
- Video-capable routes can require `supports_async_jobs = true`.
- Brand-safe generation can require `supports_brand_grounding = true`.

## Why This Matters

- Tenants should not need to care whether `image-standard` is backed by Stability today and another vendor later.
- Billing should stay attached to platform aliases, not vendor naming churn.
- Access control should target platform models, not direct vendor model ids.
