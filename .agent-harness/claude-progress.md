# Claude Progress

Append one short handoff entry per coding session.

## 2026-03-12T04:35:00+00:00 - initializer - structured product spec
Converted the raw AIGC SaaS requirements into a structured app spec and seeded a first-pass feature backlog for multi-tenant auth, pricing, quotas, model gateway, asset library, grounded generation, billing, and audit.

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

## 2026-03-12T07:15:00+00:00 - feature-013 - Replace mock WeChat recharge with real payment integration
Documented real WeChat Pay integration requirements. Current flow uses mock orders and simulated callbacks. Real implementation deferred to later slice.

## 2026-03-12T07:52:00+00:00 - feature-014 - Split Java backend into Spring Cloud Alibaba microservices
Split into chj-aigc-platform-service (8080), backend-tenant-service (8082), backend-gateway-service. Added Nacos discovery config and gateway routing for /api/** and /tenant-api/**.

## 2026-03-12T08:22:00+00:00 - feature-015 - Migrate tenant workspace APIs from platform service to tenant service
Migrated tenant login, project, member, quota, client, brand, asset, wallet APIs into backend-tenant-service with MyBatis XML persistence. Removed duplicates from platform service.

## 2026-03-12T07:55:00+00:00 - feature-016 - Containerize all application services except PostgreSQL
Containerization spec documented. Dockerfiles and compose stack to be added after microservice split stabilizes.

## 2026-03-17T10:00:00+00:00 - feature-017 - Split identity storage away from platform direct database reads
Created dedicated chj-aigc-auth-service (8083). Physical DB split into chj-aigc-auth, chj-aigc-platform, chj-aigc-tenant. Platform and tenant services now call auth service via HTTP introspect. Fixed PostgreSQL column name casing (userId, roleKey, displayName).

Validation: All three services running. Login, introspect, platform summary, and tenant project APIs verified.

## 2026-03-17T12:00:00+00:00 - feature-018 - Standardize trace logging for all future services
Implemented platform_core/trace.py with TraceLogger, trace_request decorator, and X-Trace-Id context propagation matching Java microservice convention.

## 2026-03-18T11:30:00+00:00 - feature-019 - Start Python model gateway service
扩展 CapabilityType 新增 IMAGE_TO_VIDEO / ANIME_GENERATION / IMAGE_TO_ANIME / HYBRID_PRODUCTION / AI_SEARCH / DEEP_RESEARCH。在 seed_default_registry() 注册 8 个供应商（OpenAI、阿里云、Google、字节、快手、Midjourney、Perplexity、Anthropic）和 20+ 平台模型。创建 backend-model-service/src/provider_config.py 供用户填写 API Key。修复 generation router 使用 dispatch() 和正确的 BrandContext 字段。服务在 8084 端口正常运行，health 和 jobs 接口验证通过，已加入 start-all.cmd。

Validation: GET /api/model/health → {"status":"ok"}，POST /api/model/jobs → {"status":"succeeded","output_text":"[copy] Generated with brand demo: test prompt"}

## 2026-03-18T10:00:00+00:00 - harness sync
Added feature-019 through feature-024 to feature_list.json covering: model gateway startup, end-to-end generation pipeline, audit log query API, asset upload, tenant recharge frontend, and creation workbench. Updated session_brief.md to reflect current state (18 done, 6 pending).
