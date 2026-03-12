# V1 Delivery Plan

## Objective

Turn the implemented domain core into a user-visible product in the shortest path that preserves architecture quality.

## What Is Already Implemented

### Java business side
- identity and RBAC
- tenant wallet and quota domain
- model access policy engine
- client, brand, and asset library domain
- admin audit and safety policy domain

### Python AI side
- model registry and provider bindings
- prompt grounding
- generation orchestration
- settlement and quota enforcement
- generation observability

## What Is Not Yet User-Visible

- no running Spring Boot application
- no REST API layer
- no database persistence
- no frontend console
- no real provider API adapter implementation

## Shortest Path To First UI

### Phase 1: Java service shell

Build:
- Spring Boot app
- package structure for admin, tenant, billing, asset, and model-policy APIs
- in-memory adapters first, persistence second

Output:
- backend starts locally
- health endpoint works

### Phase 2: Admin and tenant CRUD APIs

Build:
- tenants
- projects
- wallet balance and recharge
- quota management
- model access policies
- client, brand, and asset endpoints

Output:
- basic API workflows usable from Swagger or Postman

### Phase 3: Minimal frontend shell

Build:
- login shell
- admin navigation
- tenant navigation
- policy management page
- wallet/quota page
- brand/asset page

Output:
- browser-visible console

### Phase 4: Generation workbench

Build:
- submit generation request
- show prompt/audit summary
- show result
- show async video status
- show settlement result

Output:
- first real user-facing AIGC workflow

## Engineering Recommendation

The next task after this document should be:

1. Create Spring Boot application skeleton in `backend-java`
2. Expose one concrete admin API set
3. Add the first frontend shell

Anything else delays visibility without improving product leverage as much.
