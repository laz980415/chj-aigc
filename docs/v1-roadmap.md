# V1 Roadmap

## Objective

Turn the initial architecture into an implementation sequence that a coding agent can execute incrementally.

## Slice 1: Foundation

Includes:
- Identity and tenant model
- Project model
- RBAC matrix
- Core architecture decision records

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
