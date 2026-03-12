# V1 Roadmap

## Objective

Turn the initial architecture into an implementation sequence that a coding agent can execute incrementally.

## Slice 1: Foundation

Includes:
- Identity and tenant model
- Project model
- RBAC matrix
- Core architecture decision records
- Java business service boundary definition

Backlog mapping:
- feature-001
- feature-002

Reason:
- Everything else depends on clean tenant, project, and permission boundaries.

## Slice 2: Model Abstraction And Billing Core

Includes:
- Model provider registry
- Platform model abstraction
- Pricing units and ledger model
- Python model gateway boundary definition

Backlog mapping:
- feature-003
- feature-004

Reason:
- Generation cannot be stable until pricing and provider abstractions are decoupled.

## Slice 3: Tenant Finance And Model Access

Includes:
- Tenant wallet
- Recharge records
- Quota allocation
- Super-admin model access policy

Backlog mapping:
- feature-005
- feature-006

Reason:
- Before opening generation to users, access and spending controls must exist.

## Slice 4: Brand Grounding Layer

Includes:
- Client and brand entities
- Asset library
- Brand rules
- Prompt grounding inputs

Backlog mapping:
- feature-007
- feature-008

Reason:
- The main business value is brand-safe advertising generation, not generic prompting.

## Slice 5: Generation Execution

Includes:
- Text generation flow
- Image generation flow
- Video async workflow
- Output artifact persistence

Backlog mapping:
- feature-009

Reason:
- Once access, assets, and models are stable, generation orchestration becomes implementable.

## Slice 6: Cost Enforcement And Audit

Includes:
- Usage deduction
- Ledger settlement
- Quota enforcement
- Audit and safety observability

Backlog mapping:
- feature-010
- feature-011

Reason:
- Production readiness depends on enforceable controls and auditability.

## Slice 7: Delivery Packaging

Includes:
- Milestone review
- Demo scenarios
- Documentation for operations and onboarding

Backlog mapping:
- feature-012

Reason:
- V1 needs a coherent release story, not just raw services.

## First Coding Target

The first implementation-ready target after this feature is:

- Define a permission matrix and core entity schema for users, tenants, projects, roles, and memberships.

That maps directly to `feature-002`.

## Stack Rule

- Java should implement business APIs, persistent business state, and admin workflows.
- Python should implement AI-facing adapters, prompt pipelines, and generation workers.
- Cross-language contracts should be explicit before generation features are implemented.

## Current Completion Status

Completed backlog features:
- feature-001: architecture and bounded contexts
- feature-002: identity, tenant, project, and RBAC model
- feature-003: model provider registry and capability schema
- feature-004: platform pricing, metering, and ledger model
- feature-005: tenant wallet recharge and quota allocation
- feature-006: super-admin model access control
- feature-007: brand, client, and asset library domain
- feature-008: grounded prompt assembly
- feature-009: generation orchestration
- feature-010: usage deduction, settlement, and quota enforcement
- feature-011: audit logs, safety policies, and observability

Remaining backlog feature:
- feature-012: delivery slicing, implementation packaging, and release plan

## Deliverable Milestones

### Milestone A: Domain Core Ready

User-visible value:
- The engineering foundation is stable enough to start exposing APIs and UI without reworking the core domain.

Includes:
- identity and RBAC
- model registry
- pricing and ledger
- wallet and quota domain
- brand and asset library

Exit criteria:
- Business objects and core policy rules are covered by tests.
- Java and Python boundaries are explicit.

### Milestone B: AI Execution Core Ready

User-visible value:
- The platform can safely construct prompts, route to platform models, and track sync/async generation jobs.

Includes:
- prompt grounding
- generation orchestration
- settlement coordination
- safety evaluation

Exit criteria:
- Copy, image, and video flows are represented in code.
- Costs and quotas can be evaluated from generation outputs.

### Milestone C: Admin And Tenant API Layer

User-visible value:
- Internal teams can start using API endpoints for tenant management, model policy management, wallets, quotas, brands, and assets.

Recommended next build items:
- Java Spring Boot application shell
- REST APIs for auth, tenant admin, wallet, quota, model policy, client, brand, and asset endpoints
- DTOs and persistence adapters

Exit criteria:
- Postman or curl workflows can manage the main admin and tenant resources.

### Milestone D: First Visible UI

User-visible value:
- A super-admin and tenant-admin can open a browser and operate the platform through a basic interface.

Recommended next build items:
- admin console shell
- tenant console shell
- pages for tenants, projects, model access policies, wallet balance, quota settings, clients, brands, and assets

Exit criteria:
- At least one end-to-end admin flow is visible in UI.
- At least one tenant flow is visible in UI.

### Milestone E: Creative Workbench

User-visible value:
- Users can submit copy/image/video generation tasks with brand-safe grounding and see results plus costs.

Recommended next build items:
- generation request UI
- result list/detail UI
- async video status page
- audit and safety incident views

Exit criteria:
- A demo user can create a grounded generation task from the UI and inspect the result and its usage cost.

## Recommended Immediate Next Phase

The backlog core is now effectively complete. The highest-value next move is no longer another isolated domain model.

Recommended next phase:
1. Create a Java Spring Boot service shell in `backend-java`.
2. Expose APIs for:
   - tenant and project management
   - wallet and quota management
   - model access policy management
   - client, brand, and asset management
3. Create a minimal frontend admin shell.
4. Wire one visible admin flow first:
   - manage model access policies
   - inspect wallet balance and quotas

## Demo Scenarios For V1

### Demo 1: Super Admin Governance

1. Create or activate a tenant
2. Enable `copy-standard` and disable `video-standard` for a project
3. Observe the admin audit record

### Demo 2: Tenant Finance Control

1. Recharge tenant wallet
2. Allocate token and image quotas to a project and a user
3. Observe remaining balance and quota changes after a generation task

### Demo 3: Brand-Safe Content Generation

1. Create a client and brand
2. Upload or register brand assets and forbidden statements
3. Run a grounded copy or image generation request
4. Observe prompt audit, safety checks, and settlement data

## Release Recommendation

Do not wait for a polished full SaaS before exposing something visible.

Recommended release cadence:
- Release 0: API-only internal milestone
- Release 1: admin/tenant console for governance and finance
- Release 2: creative workbench for copy and image
- Release 3: async video and full audit/safety views
