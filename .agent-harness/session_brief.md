# Session Brief

Project: AIGC Advertising SaaS Platform

## Status Snapshot

- Pending: 6
- In progress: 0
- Blocked: 0
- Done: 18

## Completed Features (001-018)
- 001-012: Domain modeling, RBAC, billing, generation orchestration, audit (Python core + Java domain)
- 013: WeChat payment integration spec
- 014: Spring Cloud Alibaba microservice split (auth 8083, platform 8080, tenant 8082, gateway)
- 015: Tenant workspace APIs migrated to backend-tenant-service
- 016: Containerization spec
- 017: Auth service split, physical DB split (chj-aigc-auth / chj-aigc-platform / chj-aigc-tenant)
- 018: Trace logging standardized (X-Trace-Id, platform_core/trace.py)

## Recommended Next Feature

**feature-020**: End-to-end AIGC generation core pipeline
- Depends on: feature-019 (done)
- Wire real provider adapters, brand grounding, quota check, wallet deduction, result persistence

## Pending Features
- feature-020: End-to-end AIGC generation pipeline (unblocked)
- feature-021: Audit log query API + frontend (unblocked)
- feature-022: Asset file upload endpoint (unblocked)
- feature-023: Tenant recharge frontend flow (unblocked)
- feature-024: Frontend creation workbench (blocked by 020)

## Session Rules

- Work on one feature only.
- Validate before claiming completion.
- Leave a concise progress entry for the next session.
- Parallel work must be split by write ownership, not only by feature name.
- Before editing, check `git status --short` and re-read the target files to avoid overwriting another agent's fresh changes.
- Do not use root-level startup scripts; the canonical local startup path is `infra/dev/README.md` plus the PowerShell launchers in `infra/dev`.
- For `tenant-service`, use `infra/dev/start-tenant-service.ps1` as the canonical launcher. If a visible `cmd` window is required, open `cmd` and invoke that PowerShell script from there instead of reconstructing the environment variables by hand.
- The `infra/dev` PowerShell launchers now self-heal to local JDK 21 + Maven when the machine-level `JAVA_HOME` is stale. Reuse those launchers instead of hard-coding Java paths in ad hoc commands.
- If another agent is already editing the same file set, stop and pick a disjoint slice instead of merging ad hoc.
- Local startup is slow: cold-starting `Nacos + auth + platform + tenant + gateway` can take several minutes. Start them in order, verify registration with `http://localhost:8848/nacos/v1/ns/service/list`, and do not assume a failed service until it has had enough time to finish booting.
- `tenant-service` startup can exceed three minutes when cold-starting or recompiling. Check both `http://127.0.0.1:8082/api/health` and the Nacos instance list before declaring startup failure.
- For local validation, prefer checking Nacos OpenAPI and service health endpoints after each service comes up instead of waiting for the frontend to fail first.
- The live `tenant-service -> model-service` call must stay on HTTP/1.1. JDK `HttpClient` default upgrade attempts can trigger uvicorn `Unsupported upgrade request` and surface upstream as FastAPI `422 body missing`.
- Fresh local demo data only seeds `copy-standard` access for `tenant-demo`. To validate image/video generation locally, add `image-standard` and `video-standard` tenant allow rules through the platform admin API unless another agent has already seeded them in code.
- When creating a git commit for this project, use a Chinese commit message.
- When creating a git commit, include only the files changed in the current work slice; do not bundle another agent's files or unrelated local changes.
