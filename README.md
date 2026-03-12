# Long-Running Agent Harness

This repository implements a practical harness inspired by Anthropic's article
"Effective harnesses for long-running agents":
<https://www.anthropic.com/engineering/effective-harnesses-for-long-running-agents>.

It is also aligned to Anthropic's official quickstart repository:
<https://github.com/anthropics/claude-quickstarts/tree/main/autonomous-coding>.

For this project direction:
- Java is the primary language for business backend services.
- Python is used for model-provider integration and AI orchestration.

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
