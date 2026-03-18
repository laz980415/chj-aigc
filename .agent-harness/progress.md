# Progress Log

Use this file for concise handoff notes between agent sessions.

## 2026-03-12T04:36:28+00:00 - feature-001 - Define system architecture and bounded contexts
Created the initial architecture definition, bounded contexts, service layout, request flow, and phased V1 roadmap in docs/architecture.md and docs/v1-roadmap.md.

Validation: Reviewed the documents against the app spec and confirmed they cover service boundaries, core entities, assumptions, and roadmap slices.

## 2026-03-12T04:42:02+00:00 - feature-002 - Implement identity, tenant, project, and RBAC model
Implemented the core identity and RBAC domain model in platform_core/identity.py, including users, tenants, projects, memberships, built-in roles, permission evaluation, and a documented RBAC matrix.

Validation: Ran python -m unittest discover -s tests -v and verified the new identity tests cover platform, tenant, and project authorization behavior.

## 2026-03-12T04:44:23+00:00 - feature-003 - Design unified model provider registry and capability schema
Implemented a unified model provider registry in platform_core/models.py with provider definitions, platform model aliases, capability types, metering dimensions, provider bindings, route resolution, and default seeded mappings for copy, image, and video generation.

Validation: Ran python -m unittest discover -s tests -v and verified model registry tests for capability filtering, route priority, async routing, and disabled models.

## 2026-03-12T04:46:08+00:00 - feature-004 - Implement platform pricing, metering units, and ledger model
Implemented the billing core in platform_core/billing.py with platform pricing rules, metered usage inputs, charge quoting, vendor reference snapshots, immutable ledger entries, recharge handling, and seeded pricing for copy, image, and video capabilities.

Validation: Ran python -m unittest discover -s tests -v and verified billing tests for token pricing, image usage settlement, and historical ledger stability after price changes.

## 2026-03-12T04:52:57+00:00 - feature-005 - Implement tenant wallet recharge and quota allocation
Implemented a Java Maven module for tenant finance with wallet ledger entries, recharge and usage deduction services, quota allocations for project and user scopes, and quota consumption checks across token, image, video, daily-request, and concurrency dimensions.

Validation: Ran mvn test in backend-java and python -m unittest discover -s tests -v in the repository root; both test suites passed.

## 2026-03-12T04:58:53+00:00 - feature-006 - Implement super-admin model access control
Implemented Java-side model access control with scoped allow/deny rules for tenants, projects, and roles, plus deterministic policy evaluation and audit events for rule creation and disable actions.

Validation: Ran mvn test in backend-java and python -m unittest discover -s tests -v; Java tests verified project-over-tenant precedence, role-based grants, default deny behavior, and audit event creation.

## 2026-03-12T05:01:27+00:00 - feature-007 - Implement brand, client, and asset library domain
Implemented the Java asset-library domain with clients, brands, brand rules, assets, brand profile aggregation, and asset filtering by tenant, project, client, brand, kind, and tags.

## 2026-03-12T05:03:11+00:00 - feature-008 - Implement grounded prompt assembly for brand-safe generation
Implemented Python-side grounded prompt assembly with brand context, structured brand rules, referenced assets, generation intent, and audit metadata for traceable prompt construction.

Validation: Ran python -m unittest discover -s tests -v and mvn test; grounding tests verified brand-rule injection, asset references, and asset count limiting for traceability.

## 2026-03-12T05:05:03+00:00 - feature-009 - Implement generation job orchestration for copy, image, and video
Implemented Python-side generation orchestration with model-route resolution, grounded prompt assembly, provider invocation payload construction, synchronous copy and image dispatch, and asynchronous video job lifecycle handling.

Validation: Ran python -m unittest discover -s tests -v and mvn test; orchestration tests verified sync copy/image results, async video pending/completion flow, and grounded provider invocation payloads.

## 2026-03-12T05:08:00+00:00 - feature-010 - Implement usage deduction, settlement, and quota enforcement
Implemented Python-side settlement coordination that maps generation results into metered usage, enforces project and user quotas, applies internal pricing, and records tenant ledger deductions.

Validation: Ran python -m unittest discover -s tests -v and mvn test; settlement tests verified copy/image billing, quota consumption, and quota violation blocking before deduction.

## 2026-03-12T05:11:09+00:00 - feature-011 - Implement audit logs, safety policies, and admin observability
Implemented Java-side admin observability with admin audit events, safety policies, and safety incidents, plus Python-side generation audit records and forbidden-term safety evaluation for model outputs.

Validation: Ran mvn test and python -m unittest discover -s tests -v; observability tests verified admin event recording, safety incident capture, generation summaries, charge visibility, and safety policy violation detection.

## 2026-03-12T05:12:41+00:00 - feature-012 - Define V1 delivery slices and implementation roadmap
Expanded the V1 roadmap into a concrete delivery plan with milestone slices, demo scenarios, release recommendations, and an explicit next-phase shift from domain modeling to Spring Boot APIs and a first visible admin UI.

Validation: Validated the harness state with python harness.py doctor and reviewed that the roadmap now maps completed core features to a user-visible delivery sequence.

## 2026-03-18T05:00:00+00:00 - feature-020 - Validate end-to-end generation pipeline slices
Validated the tenant-service generation path with Java 21 (`GenerationControllerTest`, `TenantWorkspaceSmokeTest`, `HealthControllerTest`) after overriding a stale local `JAVA_HOME` that still pointed to JDK 8. Added Python-side regression coverage for `backend-model-service` job routing and `OpenAIProviderAdapter` fallback/configured branches in new root tests to keep future edits off the hot files already being changed elsewhere.

Validation: Ran `python -m unittest tests.test_model_service_router tests.test_openai_adapter -v` and `mvn test "-Dtest=GenerationControllerTest,TenantWorkspaceSmokeTest,HealthControllerTest"` with `JAVA_HOME=D:\ProgramFiles\jdk\jdk21`.

## 2026-03-18T05:10:00+00:00 - feature-021 - Add backend audit event query slice
Added a disjoint backend-only slice for audit log querying in platform service: new `GET /api/admin/audit-events` controller plus a read-only query service that supports `tenantId`, `eventType`, `startAt`, `endAt`, `page`, and `pageSize`. The current tenant filter is explicitly derived only from tenant-scoped audit events because the persisted audit record does not yet store a dedicated tenant column. Frontend work remains unclaimed because `frontend-admin/src/App.vue` is already being edited elsewhere.

Validation: Ran `mvn test "-Dtest=AdminAuditQueryControllerTest"` with `JAVA_HOME=D:\ProgramFiles\jdk\jdk21`.

## 2026-03-18T05:20:00+00:00 - feature-022 - Storage note before asset upload implementation
Confirmed the current implementation target remains local-disk raw file storage to match the existing acceptance criteria. Important handoff note: treat local disk only as the current delivery slice, not the long-term architecture. Uploaded source files should stay behind a storage abstraction so they can be migrated later to COS or another object store when asset volume grows beyond single-machine capacity. Parsed chunks and embeddings should still be modeled independently from raw file storage so the future switch only changes source object persistence, not chunk indexing contracts.

## 2026-03-18T05:30:00+00:00 - feature-022 - Add backend asset upload entry slice
Added a backend-only upload slice in tenant service without touching the hot workspace controller file set: `POST /api/tenant/assets/upload` now lands files on local disk, infers or accepts asset kind, validates tenant project/brand ownership, persists asset metadata, and calls a dedicated ingestion abstraction. The current ingestion implementation is intentionally `NoOp` so later agents can plug in document chunking, embeddings, and ES indexing behind the same interface. Raw file storage is already isolated inside the upload service so future migration to COS/object storage should not require changing the controller contract.

Validation: Ran `mvn test "-Dtest=AssetUploadServiceTest,TenantAssetUploadControllerTest"` with `JAVA_HOME=D:\ProgramFiles\jdk\jdk21`.

## 2026-03-18T05:45:00+00:00 - feature-022 - Add Python parsing and vector-index slices
Added a model-service-side parsing/indexing module behind stable abstractions: parser registry, deterministic embedding provider, chunk index interface, semantic retriever, JSONL development index, and Elasticsearch-backed index implementation. Also exposed independent model-gateway endpoints for `POST /api/model/assets/ingest` and `POST /api/model/assets/search`. On the tenant side, a remote ingestion pipeline bean can now be enabled with `tenant.asset-upload.ingestion.mode=remote`; otherwise upload still falls back to `NoOp` ingestion so local development remains non-blocking when the model service is not running.

Validation: Ran `python -m unittest tests.test_asset_ingestion tests.test_asset_ingestion_router -v` and `mvn test "-Dtest=AssetUploadServiceTest,TenantAssetUploadControllerTest"` with `JAVA_HOME=D:\ProgramFiles\jdk\jdk21`.

## 2026-03-18T05:55:00+00:00 - feature-022 - Add semantic grounding retrieval slice
Added a dedicated semantic grounding service in model service so brand grounding no longer depends only on coarse asset metadata. New endpoint `POST /api/model/assets/grounding-context` retrieves top semantic chunks for the selected asset set and returns structured snippets that can be merged into prompt assembly later without changing the retrieval contract. This slice is intentionally isolated from `generation.py`, which is currently being edited elsewhere, so later integration should happen by calling the new grounding service instead of re-implementing retrieval logic inside the generation router.

Validation: Ran `python -m unittest tests.test_asset_ingestion tests.test_asset_ingestion_router tests.test_asset_grounding -v`.

## 2026-03-18T06:05:00+00:00 - feature-022 - Add tenant-side grounding client and service wiring
Added a tenant-side semantic grounding abstraction so later generation integration can stay inside domain services instead of scattering ad hoc HTTP calls. New Java components include `AssetGroundingClient`, `RemoteAssetGroundingClient`, `TenantAssetGroundingService`, and a standalone `AssetGroundingConfig` that registers the remote client/service without editing the already-hot `ApplicationConfig`. This means future work on generation can inject a ready-made grounding service and focus only on orchestration decisions.

Validation: Ran `mvn test "-Dtest=AssetUploadServiceTest,TenantAssetUploadControllerTest,RemoteAssetGroundingClientTest,TenantAssetGroundingServiceTest,AssetGroundingConfigTest"` with `JAVA_HOME=D:\ProgramFiles\jdk\jdk21`.

## 2026-03-18T06:46:00+00:00 - feature-020 - Close tenant-service generation orchestration slice
Validated and closed the tenant-service half of the generation pipeline in `backend-tenant-service`: generation job submission/query APIs, model-access check, remote model-gateway calls pinned to HTTP/1.1, settlement persistence, schema/mapper wiring, and the matching MVC regression tests. This slice is ready to be committed independently from the still-open Python/provider/admin/frontend sections.

Validation: Ran `mvn test "-Dtest=GenerationControllerTest,TenantWorkspaceSmokeTest,HealthControllerTest"` with `JAVA_HOME=D:\ProgramFiles\jdk\jdk21`.
