# Unclosed Work TODO

## Purpose

This note is a handoff inventory for the remaining uncommitted files that were still present in the workspace after:

- `08fec86` `修复本地联调启动与模型网关调用`
- `8e41ba2` `实现素材上传与语义检索链路`

It exists so the remaining code can be closed in a small number of clean commits instead of leaving the workspace in a mixed state.

## How To Use This Note

- Treat each section below as a separate cleanup unit.
- Do not batch frontend, platform admin, and tenant generation changes into one commit.
- Prefer one feature-facing commit per section after local validation passes.
- Do not modify `.agent-harness/session_brief.md` or `.agent-harness/feature_list.json` again until the matching code commit is ready.

## Section A

Feature guess: `feature-020` generation mainline and provider wiring.

Files:

- [backend-model-service/src/routers/generation.py](/E:/ai-workspaces/backend-model-service/src/routers/generation.py)
- [backend-model-service/src/openai_adapter.py](/E:/ai-workspaces/backend-model-service/src/openai_adapter.py)
- [backend-model-service/src/provider_config.py](/E:/ai-workspaces/backend-model-service/src/provider_config.py)
- [platform_core/generation.py](/E:/ai-workspaces/platform_core/generation.py)
- [platform_core/models.py](/E:/ai-workspaces/platform_core/models.py)
- [tests/test_models.py](/E:/ai-workspaces/tests/test_models.py)

Why this is still open:

- This is the Python-side heart of the generation pipeline.
- It spans routing, provider binding, adapter behavior, and model registry.
- It should be closed together with a focused validation pass, not mixed with unrelated upload or frontend work.

Suggested close-out:

- Verify copy, image, and video paths against the current provider/router behavior.
- Re-run Python tests for models and generation together.
- Commit as one generation-focused commit.

Validation target:

- `python -m unittest tests.test_models tests.test_generation tests.test_model_service_router tests.test_openai_adapter -v`

## Section B

Feature guess: `feature-020` tenant-service generation orchestration and persistence.

Files:

- [backend-tenant-service/src/main/java/com/chj/aigc/tenantservice/generation/GenerationCapability.java](/E:/ai-workspaces/backend-tenant-service/src/main/java/com/chj/aigc/tenantservice/generation/GenerationCapability.java)
- [backend-tenant-service/src/main/java/com/chj/aigc/tenantservice/generation/GenerationJob.java](/E:/ai-workspaces/backend-tenant-service/src/main/java/com/chj/aigc/tenantservice/generation/GenerationJob.java)
- [backend-tenant-service/src/main/java/com/chj/aigc/tenantservice/generation/GenerationJobStatus.java](/E:/ai-workspaces/backend-tenant-service/src/main/java/com/chj/aigc/tenantservice/generation/GenerationJobStatus.java)
- [backend-tenant-service/src/main/java/com/chj/aigc/tenantservice/generation/GenerationService.java](/E:/ai-workspaces/backend-tenant-service/src/main/java/com/chj/aigc/tenantservice/generation/GenerationService.java)
- [backend-tenant-service/src/main/java/com/chj/aigc/tenantservice/generation/GenerationStore.java](/E:/ai-workspaces/backend-tenant-service/src/main/java/com/chj/aigc/tenantservice/generation/GenerationStore.java)
- [backend-tenant-service/src/main/java/com/chj/aigc/tenantservice/generation/ModelAccessClient.java](/E:/ai-workspaces/backend-tenant-service/src/main/java/com/chj/aigc/tenantservice/generation/ModelAccessClient.java)
- [backend-tenant-service/src/main/java/com/chj/aigc/tenantservice/generation/ModelGatewayClient.java](/E:/ai-workspaces/backend-tenant-service/src/main/java/com/chj/aigc/tenantservice/generation/ModelGatewayClient.java)
- [backend-tenant-service/src/main/java/com/chj/aigc/tenantservice/generation/MybatisGenerationStore.java](/E:/ai-workspaces/backend-tenant-service/src/main/java/com/chj/aigc/tenantservice/generation/MybatisGenerationStore.java)
- [backend-tenant-service/src/main/java/com/chj/aigc/tenantservice/generation/RemoteModelAccessClient.java](/E:/ai-workspaces/backend-tenant-service/src/main/java/com/chj/aigc/tenantservice/generation/RemoteModelAccessClient.java)
- [backend-tenant-service/src/main/java/com/chj/aigc/tenantservice/persistence/mapper/GenerationMapper.java](/E:/ai-workspaces/backend-tenant-service/src/main/java/com/chj/aigc/tenantservice/persistence/mapper/GenerationMapper.java)
- [backend-tenant-service/src/main/java/com/chj/aigc/tenantservice/web/dto/CreateGenerationJobRequest.java](/E:/ai-workspaces/backend-tenant-service/src/main/java/com/chj/aigc/tenantservice/web/dto/CreateGenerationJobRequest.java)
- [backend-tenant-service/src/main/resources/mapper/GenerationMapper.xml](/E:/ai-workspaces/backend-tenant-service/src/main/resources/mapper/GenerationMapper.xml)
- [backend-tenant-service/src/test/java/com/chj/aigc/tenantservice/web/GenerationControllerTest.java](/E:/ai-workspaces/backend-tenant-service/src/test/java/com/chj/aigc/tenantservice/web/GenerationControllerTest.java)

Paired modified files in the same area:

- [backend-tenant-service/src/main/java/com/chj/aigc/tenantservice/billing/TenantBillingService.java](/E:/ai-workspaces/backend-tenant-service/src/main/java/com/chj/aigc/tenantservice/billing/TenantBillingService.java)
- [backend-tenant-service/src/main/java/com/chj/aigc/tenantservice/web/ApplicationConfig.java](/E:/ai-workspaces/backend-tenant-service/src/main/java/com/chj/aigc/tenantservice/web/ApplicationConfig.java)
- [backend-tenant-service/src/main/java/com/chj/aigc/tenantservice/web/TenantApiController.java](/E:/ai-workspaces/backend-tenant-service/src/main/java/com/chj/aigc/tenantservice/web/TenantApiController.java)
- [backend-tenant-service/src/main/resources/application.yml](/E:/ai-workspaces/backend-tenant-service/src/main/resources/application.yml)
- [backend-tenant-service/src/main/resources/schema.sql](/E:/ai-workspaces/backend-tenant-service/src/main/resources/schema.sql)
- [backend-tenant-service/src/test/java/com/chj/aigc/tenantservice/web/HealthControllerTest.java](/E:/ai-workspaces/backend-tenant-service/src/test/java/com/chj/aigc/tenantservice/web/HealthControllerTest.java)
- [backend-tenant-service/src/test/java/com/chj/aigc/tenantservice/web/TenantWorkspaceSmokeTest.java](/E:/ai-workspaces/backend-tenant-service/src/test/java/com/chj/aigc/tenantservice/web/TenantWorkspaceSmokeTest.java)

Why this is still open:

- This is a single coherent Java slice and should not be split across several commits.
- It includes controller, billing settlement, DB mapping, and remote model calls.
- The file set is already large enough that someone likely paused before final validation/commit.

Suggested close-out:

- Run only the tenant-service generation-related tests under Java 21.
- Confirm schema and mapper changes align.
- Commit as one tenant generation commit.

Validation target:

- `mvn test "-Dtest=GenerationControllerTest,TenantWorkspaceSmokeTest,HealthControllerTest"`

## Section C

Feature guess: platform provider config and internal admin endpoints.

Files:

- [chj-aigc-platform-service/src/main/java/com/chj/aigc/provider/MybatisProviderConfigStore.java](/E:/ai-workspaces/chj-aigc-platform-service/src/main/java/com/chj/aigc/provider/MybatisProviderConfigStore.java)
- [chj-aigc-platform-service/src/main/java/com/chj/aigc/provider/ProviderConfig.java](/E:/ai-workspaces/chj-aigc-platform-service/src/main/java/com/chj/aigc/provider/ProviderConfig.java)
- [chj-aigc-platform-service/src/main/java/com/chj/aigc/provider/ProviderConfigStore.java](/E:/ai-workspaces/chj-aigc-platform-service/src/main/java/com/chj/aigc/provider/ProviderConfigStore.java)
- [chj-aigc-platform-service/src/main/java/com/chj/aigc/web/InternalApiController.java](/E:/ai-workspaces/chj-aigc-platform-service/src/main/java/com/chj/aigc/web/InternalApiController.java)
- [chj-aigc-platform-service/src/main/java/com/chj/aigc/web/dto/UpsertProviderConfigRequest.java](/E:/ai-workspaces/chj-aigc-platform-service/src/main/java/com/chj/aigc/web/dto/UpsertProviderConfigRequest.java)
- [chj-aigc-platform-service/src/main/java/com/chj/aigc/web/AdminApiController.java](/E:/ai-workspaces/chj-aigc-platform-service/src/main/java/com/chj/aigc/web/AdminApiController.java)
- [chj-aigc-platform-service/src/main/resources/schema.sql](/E:/ai-workspaces/chj-aigc-platform-service/src/main/resources/schema.sql)

Why this is still open:

- This is another independent slice, likely needed by the model gateway/provider setup.
- It should be reviewed as a platform-admin feature, not mixed into tenant generation.

Suggested close-out:

- Verify admin API serialization and provider schema changes together.
- Confirm any internal endpoint contract used by tenant/model services.
- Commit as one platform provider/admin commit.

## Section D

Feature guess: frontend admin or tenant workbench follow-up.

Files:

- [frontend-admin/src/App.vue](/E:/ai-workspaces/frontend-admin/src/App.vue)
- [frontend-admin/screenshot.mjs](/E:/ai-workspaces/frontend-admin/screenshot.mjs)
- [frontend-admin/screenshot-login.png](/E:/ai-workspaces/frontend-admin/screenshot-login.png)

Why this is still open:

- Frontend should be closed after backend APIs stabilize.
- Screenshot artifacts should not be committed unless they are intentionally part of the repo history.

Suggested close-out:

- Decide whether screenshot files are temporary debug artifacts.
- If yes, drop them before commit.
- If no, keep them in a dedicated UI verification commit.

## Section E

Workspace metadata and harness notes.

Files:

- [/.agent-harness/claude-progress.md](/E:/ai-workspaces/.agent-harness/claude-progress.md)
- [/.agent-harness/feature_list.json](/E:/ai-workspaces/.agent-harness/feature_list.json)
- [/.agent-harness/session_brief.md](/E:/ai-workspaces/.agent-harness/session_brief.md)
- [/.vscode/settings.json](/E:/ai-workspaces/.vscode/settings.json)

Why this is still open:

- These are coordination files, not product code.
- They should be updated only after the corresponding code section is actually committed.

Suggested close-out:

- Keep them out of product commits.
- If needed, make one final workspace metadata commit after code cleanup is done.

## Section F

Cross-service auth storage drift.

Files:

- [chj-aigc-auth-service/src/main/java/com/chj/aigc/authservice/auth/MybatisAuthStore.java](/E:/ai-workspaces/chj-aigc-auth-service/src/main/java/com/chj/aigc/authservice/auth/MybatisAuthStore.java)

Why this is still open:

- Single-file backend auth change with no obvious paired test in the current status list.
- Needs explicit owner confirmation before commit.

Suggested close-out:

- Check what behavior changed.
- Add or run the matching auth-service test before committing.

## Recommended Order

1. Close Section B if feature-020 is the priority.
2. Close Section A right after, because it is the Python half of the same flow.
3. Close Section C if provider config/internal admin APIs are needed for production wiring.
4. Close Section D only after backend contracts stop moving.
5. Leave Section E for the end.

## Important Note

The current workspace state does not look like random noise. It looks like two partially finished feature slices plus harness metadata. The cleanup strategy should therefore be "group by coherent feature and validate", not "blindly commit everything that is left".
