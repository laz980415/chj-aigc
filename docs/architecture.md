# AIGC Advertising SaaS Architecture

## Goal

Build a multi-tenant AIGC SaaS platform for advertising agencies and their brand clients.
The platform must support tenant isolation, project-level collaboration, unified model access,
internal pricing, quota control, asset grounding, and auditable generation workflows.

## V1 Principles

- Multi-tenant isolation is mandatory.
- Business-facing APIs must not depend on any single model vendor.
- Billing must be based on internal platform pricing rather than vendor list prices.
- Brand assets and rules must be first-class inputs to generation.
- Video generation must be treated as an asynchronous workflow.
- All high-cost and high-risk actions must be auditable.

## Bounded Contexts

### 1. Identity And Access

Responsibilities:
- User accounts
- Authentication sessions
- Tenant membership
- Project membership
- Role-based access control

Core entities:
- User
- Tenant
- TenantMembership
- Project
- ProjectMembership
- Role
- Permission

Key rules:
- A user may belong to multiple projects within one tenant.
- Platform super admins are outside tenant scope.
- Project permissions inherit tenant context but can be narrowed.

### 2. Tenant Billing

Responsibilities:
- Tenant wallet
- Recharge records
- Internal pricing configuration
- Usage metering
- Ledger and settlement
- Quota policies

Core entities:
- Wallet
- RechargeOrder
- PricingPlan
- PriceRule
- UsageRecord
- LedgerEntry
- QuotaPolicy
- QuotaSnapshot

Key rules:
- Charges are always calculated in platform-defined units.
- Historical ledger entries are immutable.
- Quota checks must happen before model execution where possible.

### 3. Model Gateway

Responsibilities:
- Provider registration
- Model registry
- Capability abstraction
- Vendor adapter execution
- Fallback and vendor replacement

Core entities:
- Provider
- ProviderCredential
- PlatformModel
- ProviderModelBinding
- CapabilityType
- GenerationRequest
- GenerationResult

Key rules:
- Business services call platform models, not vendor-specific models.
- One platform model may map to different provider models over time.
- Every request must preserve both platform model id and actual vendor model id.

### 4. Brand And Asset Library

Responsibilities:
- Brand client records
- Brand rules and voice constraints
- Asset metadata
- Project-brand bindings
- Material retrieval for generation

Core entities:
- Client
- Brand
- BrandRule
- Asset
- AssetCollection
- AssetReference
- ProjectBrandBinding

Key rules:
- Assets belong to a tenant and can be scoped to projects or brands.
- Brand rules are applied as structured constraints rather than free text only.
- Future retrieval features must build on the same asset metadata layer.

### 5. Generation Orchestration

Responsibilities:
- Task creation
- Prompt assembly
- Context injection
- Sync text/image execution
- Async video execution
- Result persistence

Core entities:
- GenerationTask
- PromptTemplate
- PromptContext
- JobStatus
- OutputArtifact

Key rules:
- Every task references tenant, project, actor, and brand context.
- Prompt assembly must be reproducible for audit.
- Async jobs must support polling, callback, or scheduled reconciliation.

### 6. Governance And Audit

Responsibilities:
- Admin action audit
- Generation audit
- Safety checks
- Sensitive content handling
- Cost and usage observability

Core entities:
- AuditEvent
- SafetyPolicy
- SafetyIncident
- AccessLog

Key rules:
- Admin changes are logged with actor, target, before, and after states.
- Generation audit stores summaries and metadata, not unrestricted raw secrets.
- High-cost model usage should be queryable by tenant, project, and user.

## Logical Service Layout

### auth-service
- Users, tenants, projects, memberships, roles

### billing-service
- Wallet, recharge, quota, pricing, ledger

### model-gateway
- Providers, adapters, platform models, capability routing

### asset-service
- Clients, brands, brand rules, assets, collections

### generation-service
- Task orchestration, prompt assembly, job execution, outputs

### admin-console
- Super admin UI for models, tenants, pricing, governance

### tenant-console
- Tenant/project UI for users, budgets, assets, generation tasks

## Suggested Data Relationships

- Tenant 1..n Project
- Tenant 1..n UserMembership
- Project 1..n ProjectMembership
- Tenant 1..n Client
- Client 1..n Brand
- Brand 1..n BrandRule
- Tenant 1..n Asset
- Project n..n Brand
- Tenant 1..1 Wallet
- Tenant 1..n QuotaPolicy
- PlatformModel n..n ProviderModelBinding
- GenerationTask n..1 PlatformModel
- GenerationTask n..1 Project
- GenerationTask n..1 Brand
- GenerationTask 1..n OutputArtifact
- GenerationTask 1..n UsageRecord

## High-Level Request Flow

1. User authenticates and selects a tenant and project.
2. Platform checks membership and role permissions.
3. User submits a generation request with brand and asset references.
4. Generation service loads brand rules and relevant assets.
5. Model access policy verifies allowed models for the current scope.
6. Billing service performs quota pre-check.
7. Generation service builds prompt context and calls the platform model.
8. Model gateway routes to the configured vendor adapter.
9. Result and usage are persisted.
10. Billing service writes ledger entries and quota consumption.
11. Audit service records the event.

## V1 Technical Assumptions

- Use API-based third-party model platforms only.
- Recharge can start as manual back-office crediting.
- Asset storage can start with object storage plus relational metadata.
- Retrieval augmentation can be introduced later without breaking asset APIs.
- Video generation is asynchronous from day one.

## Risks

- Pricing complexity grows quickly across modalities and vendors.
- Brand grounding quality depends on asset quality and prompt assembly discipline.
- Async video vendors often have inconsistent callback behavior.
- Tenant-level quota models can become difficult to explain if pricing is opaque.

## Out Of Scope For Feature-001

- Selecting the final programming language or framework
- Implementing database schema
- Implementing vendor adapters
- Building UI pages
