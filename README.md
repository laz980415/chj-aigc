# Long-Running Agent Harness

This repository implements a practical harness inspired by Anthropic's article
"Effective harnesses for long-running agents":
<https://www.anthropic.com/engineering/effective-harnesses-for-long-running-agents>.

It is also aligned to Anthropic's official quickstart repository:
<https://github.com/anthropics/claude-quickstarts/tree/main/autonomous-coding>.

For this project direction:
- Java is the primary language for business backend services.
- Python is used for model-provider integration and AI orchestration.
- Java backend is moving to a Spring Cloud Alibaba microservice architecture with Nacos discovery.

## Backend Status

The repository now includes:

- `backend-java`: platform management service
- `backend-tenant-service`: tenant workspace microservice skeleton
- `backend-gateway-service`: Spring Cloud Gateway entry service

Current API shell endpoints:
- `GET /api/health`
- `GET /api/db-info`
- `GET /api/admin/summary`
- `GET /api/admin/model-access-rules`
- `GET /api/tenant/wallet`
- `GET /api/tenant/quotas`
- `GET /api/tenant/clients`
- `GET /api/tenant/brands/{clientId}`
- `GET /api/tenant/assets`

## Run Java Services

Requirements:
- JDK `21`
- Maven using local repository `E:\repository`
- Optional Nacos server for service discovery

Start local Nacos for development:

```powershell
Set-Location infra\nacos
docker compose up -d
```

Optional environment variables:
- `APP_DB_URL`
- `APP_DB_USERNAME`
- `APP_DB_PASSWORD`
- `NACOS_DISCOVERY_ENABLED`
- `NACOS_SERVER_ADDR`
- `NACOS_NAMESPACE`
- `NACOS_GROUP`

Run the platform service:

```powershell
$env:APP_DB_URL="jdbc:postgresql://36.150.108.207:54312/chj-aigc"
$env:APP_DB_USERNAME="postgres"
$env:APP_DB_PASSWORD="your-password"
$env:NACOS_DISCOVERY_ENABLED="false"
Set-Location backend-java
mvn spring-boot:run "-Dmaven.repo.local=E:\repository"
```

If you want to register services into Nacos instead of local direct routing:

```powershell
$env:NACOS_DISCOVERY_ENABLED="true"
$env:NACOS_SERVER_ADDR="127.0.0.1:8848"
```

Run the tenant microservice skeleton:

```powershell
$env:NACOS_DISCOVERY_ENABLED="false"
Set-Location backend-tenant-service
mvn spring-boot:run "-Dmaven.repo.local=E:\repository"
```

Run the gateway service:

```powershell
$env:NACOS_DISCOVERY_ENABLED="false"
Set-Location backend-gateway-service
mvn spring-boot:run "-Dmaven.repo.local=E:\repository"
```

After startup, open:

```text
http://127.0.0.1:8080/api/health
http://127.0.0.1:8081/gateway/health
http://127.0.0.1:8080/api/admin/summary
http://127.0.0.1:8080/api/tenant/wallet
http://127.0.0.1:8082/api/health
```

Run Java tests with:

```powershell
Set-Location backend-java
mvn test "-Dmaven.repo.local=E:\repository"

Set-Location ..\backend-tenant-service
mvn test "-Dmaven.repo.local=E:\repository"

Set-Location ..\backend-gateway-service
mvn test "-Dmaven.repo.local=E:\repository"
```

## Run Frontend

The admin console now uses Vue 3 with a separate Vite dev server in `frontend-admin`.

```powershell
Set-Location frontend-admin
npm install
npm run dev
```

Open:

```text
http://127.0.0.1:5173
```

The Vite dev server now proxies `/api` requests to the gateway service at `http://127.0.0.1:8081`.

Current local call path is:

- `frontend-admin` -> `backend-gateway-service`
- `backend-gateway-service` -> `backend-tenant-service` for `/api/auth/**` and `/api/tenant/**`
- `backend-gateway-service` -> `backend-java` for platform APIs under `/api/**`

## Microservice Notes

The detailed migration notes are documented in `docs/microservice-architecture.md`.
Recommended next databases on the same PostgreSQL server are:

- `chj-aigc-platform`
- `chj-aigc-tenant`

The core idea is simple:

- An initializer step creates durable project artifacts.
- A coding session works on exactly one feature at a time.
- Each session leaves behind structured state for the next session.

This project provides a small CLI that manages those artifacts for any repo.

## What it creates

Running `python harness.py init` creates:

- `.agent-harness/config.json`
- `.agent-harness/features.json`
- `.agent-harness/feature_list.json`
- `.agent-harness/progress.md`
- `.agent-harness/claude-progress.md`
- `.agent-harness/app_spec.md`
- `.agent-harness/security.json`
- `.agent-harness/prompts/initializer_prompt.md`
- `.agent-harness/prompts/coding_prompt.md`
- `.agent-harness/session_brief.md`
- `.agent-harness/session_packet.md`

## Commands

```bash
python harness.py init --project "My Project"
python harness.py import-spec --file requirements.md
python harness.py plan --command-template "claude -p @'{prompt}'"
python harness.py add-feature "Create login form" --description "Email/password UI"
python harness.py add-feature "Implement auth API" --depends-on feature-001
python harness.py status
python harness.py next
python harness.py work-next --command-template "claude -p @'{prompt}'"
python harness.py complete feature-001 --summary "Built form and added validation"
python harness.py run-agent coding --command-template "claude -p @'{prompt}'"
python harness.py doctor
```

## Suggested workflow

1. Run `init` once inside the project root.
2. Put your raw requirements into a Markdown file.
3. Import them with `import-spec`.
4. Run `plan` so the initializer agent splits the spec into `feature_list.json`.
5. Run `work-next` to let the coding agent work on the next feature.
6. Use `complete` if you want to manually close a feature after validation.
7. Use `doctor` before handing the repo to a new session.

## End-to-end usage

If you already have Claude Code or a compatible CLI agent installed, the shortest path is:

```bash
python harness.py init --project "Acme Dashboard"
python harness.py import-spec --file requirements.md
python harness.py plan --command-template "claude -p @'{prompt}'" --dry-run
python harness.py work-next --command-template "claude -p @'{prompt}'" --dry-run
```

Remove `--dry-run` once the rendered command looks correct for your local agent setup.

The intended flow is:

- You write the product requirement in `requirements.md`.
- `import-spec` copies it into `.agent-harness/app_spec.md`.
- `plan` runs the initializer prompt so the agent decomposes the requirement into small features.
- `work-next` runs the coding prompt using the current brief and selected feature.
- The agent updates `feature_list.json` and `claude-progress.md` between sessions.

## Design choices

- `feature_list.json` mirrors the official quickstart naming.
- `claude-progress.md` is the durable session handoff log.
- `app_spec.md` and `security.json` make initialization explicit.
- `session_brief.md` and `session_packet.md` are regenerated for each session.
- `run-agent` can render or execute a Claude-compatible command template.

## References

- Anthropic Engineering: <https://www.anthropic.com/engineering/effective-harnesses-for-long-running-agents>
- Anthropic Quickstart: <https://github.com/anthropics/claude-quickstarts/tree/main/autonomous-coding>
- Engineering.fyi mirror: <https://www.engineering.fyi/article/effective-harnesses-for-long-running-agents>
