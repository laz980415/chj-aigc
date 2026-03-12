from __future__ import annotations

import json
import shlex
import subprocess
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

from .templates import (
    coding_prompt,
    default_app_spec,
    default_security_policy,
    initializer_prompt,
    session_packet,
)

HARNESS_DIR = ".agent-harness"
CONFIG_FILE = "config.json"
FEATURES_FILE = "features.json"
FEATURE_LIST_FILE = "feature_list.json"
PROGRESS_FILE = "progress.md"
CLAUDE_PROGRESS_FILE = "claude-progress.md"
INITIALIZER_PROMPT_FILE = "initializer_prompt.md"
CODING_PROMPT_FILE = "coding_prompt.md"
SESSION_BRIEF_FILE = "session_brief.md"
SESSION_PACKET_FILE = "session_packet.md"
APP_SPEC_FILE = "app_spec.md"
SECURITY_FILE = "security.json"
PROMPTS_DIR = "prompts"

VALID_STATES = {"pending", "in_progress", "done", "blocked"}


def now_iso() -> str:
    return datetime.now(timezone.utc).replace(microsecond=0).isoformat()


@dataclass
class HarnessPaths:
    root: Path

    @property
    def harness_dir(self) -> Path:
        return self.root / HARNESS_DIR

    @property
    def config(self) -> Path:
        return self.harness_dir / CONFIG_FILE

    @property
    def features(self) -> Path:
        return self.harness_dir / FEATURES_FILE

    @property
    def feature_list(self) -> Path:
        return self.harness_dir / FEATURE_LIST_FILE

    @property
    def progress(self) -> Path:
        return self.harness_dir / PROGRESS_FILE

    @property
    def claude_progress(self) -> Path:
        return self.harness_dir / CLAUDE_PROGRESS_FILE

    @property
    def initializer_prompt(self) -> Path:
        return self.prompts_dir / INITIALIZER_PROMPT_FILE

    @property
    def coding_prompt(self) -> Path:
        return self.prompts_dir / CODING_PROMPT_FILE

    @property
    def session_brief(self) -> Path:
        return self.harness_dir / SESSION_BRIEF_FILE

    @property
    def session_packet(self) -> Path:
        return self.harness_dir / SESSION_PACKET_FILE

    @property
    def app_spec(self) -> Path:
        return self.harness_dir / APP_SPEC_FILE

    @property
    def security(self) -> Path:
        return self.harness_dir / SECURITY_FILE

    @property
    def prompts_dir(self) -> Path:
        return self.harness_dir / PROMPTS_DIR


def resolve_paths(root: str | Path | None = None) -> HarnessPaths:
    return HarnessPaths(Path(root or ".").resolve())


def _write_json(path: Path, payload: dict[str, Any]) -> None:
    path.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")


def _read_json(path: Path) -> dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8"))


def ensure_initialized(paths: HarnessPaths) -> None:
    if not paths.harness_dir.exists():
        raise FileNotFoundError(
            f"Harness not initialized at {paths.harness_dir}. Run `python harness.py init`."
        )


def init_harness(root: str | Path, project_name: str) -> HarnessPaths:
    paths = resolve_paths(root)
    paths.harness_dir.mkdir(parents=True, exist_ok=True)
    paths.prompts_dir.mkdir(parents=True, exist_ok=True)

    if not paths.config.exists():
        _write_json(
            paths.config,
            {
                "project_name": project_name,
                "created_at": now_iso(),
                "version": 1,
            },
        )

    if not paths.features.exists():
        _write_json(
            paths.features,
            {
                "project_name": project_name,
                "created_at": now_iso(),
                "features": [],
            },
        )

    if not paths.feature_list.exists():
        _write_json(
            paths.feature_list,
            {
                "project_name": project_name,
                "created_at": now_iso(),
                "features": [],
            },
        )

    if not paths.progress.exists():
        paths.progress.write_text(
            "# Progress Log\n\n"
            "Use this file for concise handoff notes between agent sessions.\n",
            encoding="utf-8",
        )

    if not paths.claude_progress.exists():
        paths.claude_progress.write_text(
            "# Claude Progress\n\n"
            "Append one short handoff entry per coding session.\n",
            encoding="utf-8",
        )

    if not paths.app_spec.exists():
        paths.app_spec.write_text(default_app_spec(project_name), encoding="utf-8")

    if not paths.security.exists():
        paths.security.write_text(default_security_policy(), encoding="utf-8")

    if not paths.initializer_prompt.exists():
        paths.initializer_prompt.write_text(
            initializer_prompt(project_name),
            encoding="utf-8",
        )

    if not paths.coding_prompt.exists():
        paths.coding_prompt.write_text(
            coding_prompt(project_name),
            encoding="utf-8",
        )

    refresh_session_brief(paths)
    return paths


def load_config(paths: HarnessPaths) -> dict[str, Any]:
    ensure_initialized(paths)
    return _read_json(paths.config)


def write_app_spec(paths: HarnessPaths, content: str) -> None:
    ensure_initialized(paths)
    paths.app_spec.write_text(content.rstrip() + "\n", encoding="utf-8")


def load_features(paths: HarnessPaths) -> dict[str, Any]:
    ensure_initialized(paths)
    return _read_json(paths.features)


def save_features(paths: HarnessPaths, payload: dict[str, Any]) -> None:
    _write_json(paths.features, payload)
    _write_json(paths.feature_list, payload)


def next_feature_id(feature_items: list[dict[str, Any]]) -> str:
    return f"feature-{len(feature_items) + 1:03d}"


def add_feature(
    paths: HarnessPaths,
    title: str,
    description: str = "",
    acceptance_criteria: list[str] | None = None,
    depends_on: list[str] | None = None,
) -> dict[str, Any]:
    data = load_features(paths)
    feature_items = data["features"]
    feature = {
        "id": next_feature_id(feature_items),
        "title": title,
        "description": description,
        "acceptance_criteria": acceptance_criteria or [],
        "depends_on": depends_on or [],
        "state": "pending",
        "created_at": now_iso(),
        "updated_at": now_iso(),
        "session_notes": [],
    }
    feature_items.append(feature)
    save_features(paths, data)
    refresh_session_brief(paths)
    return feature


def _find_feature(data: dict[str, Any], feature_id: str) -> dict[str, Any]:
    for feature in data["features"]:
        if feature["id"] == feature_id:
            return feature
    raise KeyError(f"Feature not found: {feature_id}")


def feature_is_unblocked(feature: dict[str, Any], data: dict[str, Any]) -> bool:
    for dependency in feature.get("depends_on", []):
        try:
            dep_feature = _find_feature(data, dependency)
        except KeyError:
            return False
        if dep_feature["state"] != "done":
            return False
    return True


def select_next_feature(paths: HarnessPaths) -> dict[str, Any] | None:
    data = load_features(paths)

    for state in ("in_progress", "pending"):
        for feature in data["features"]:
            if feature["state"] == state and feature_is_unblocked(feature, data):
                return feature
    return None


def set_feature_state(
    paths: HarnessPaths,
    feature_id: str,
    state: str,
    note: str | None = None,
) -> dict[str, Any]:
    if state not in VALID_STATES:
        raise ValueError(f"Invalid state: {state}")

    data = load_features(paths)
    feature = _find_feature(data, feature_id)
    feature["state"] = state
    feature["updated_at"] = now_iso()
    if note:
        feature["session_notes"].append({"at": now_iso(), "note": note})
    save_features(paths, data)
    refresh_session_brief(paths)
    return feature


def append_progress(
    paths: HarnessPaths,
    feature_id: str,
    title: str,
    summary: str,
    validation: str | None = None,
) -> None:
    ensure_initialized(paths)
    lines = [
        f"\n## {now_iso()} - {feature_id} - {title}\n",
        f"{summary.strip()}\n",
    ]
    if validation:
        lines.append(f"\nValidation: {validation.strip()}\n")
    paths.progress.write_text(
        paths.progress.read_text(encoding="utf-8") + "".join(lines),
        encoding="utf-8",
    )
    paths.claude_progress.write_text(
        paths.claude_progress.read_text(encoding="utf-8") + "".join(lines),
        encoding="utf-8",
    )


def refresh_session_brief(paths: HarnessPaths) -> None:
    ensure_initialized(paths)
    config = load_config(paths)
    data = load_features(paths)
    next_feature = select_next_feature(paths)

    pending = [f for f in data["features"] if f["state"] == "pending"]
    in_progress = [f for f in data["features"] if f["state"] == "in_progress"]
    blocked = [f for f in data["features"] if f["state"] == "blocked"]
    done = [f for f in data["features"] if f["state"] == "done"]

    lines = [
        "# Session Brief\n\n",
        f"Project: {config['project_name']}\n\n",
        "## Status Snapshot\n\n",
        f"- Pending: {len(pending)}\n",
        f"- In progress: {len(in_progress)}\n",
        f"- Blocked: {len(blocked)}\n",
        f"- Done: {len(done)}\n\n",
    ]

    if next_feature:
        lines.extend(
            [
                "## Recommended Next Feature\n\n",
                f"- ID: {next_feature['id']}\n",
                f"- Title: {next_feature['title']}\n",
                f"- State: {next_feature['state']}\n",
            ]
        )
        if next_feature.get("description"):
            lines.append(f"- Description: {next_feature['description']}\n")
        if next_feature.get("depends_on"):
            lines.append(
                f"- Depends on: {', '.join(next_feature['depends_on'])}\n"
            )
        if next_feature.get("acceptance_criteria"):
            lines.append("- Acceptance criteria:\n")
            for item in next_feature["acceptance_criteria"]:
                lines.append(f"  - {item}\n")
        lines.append("\n")
    else:
        lines.append("## Recommended Next Feature\n\nNo unblocked feature is available.\n\n")

    lines.extend(
        [
            "## Session Rules\n\n",
            "- Work on one feature only.\n",
            "- Validate before claiming completion.\n",
            "- Leave a concise progress entry for the next session.\n",
        ]
    )

    paths.session_brief.write_text("".join(lines), encoding="utf-8")
    paths.session_packet.write_text(
        session_packet(config["project_name"], next_feature["id"] if next_feature else None),
        encoding="utf-8",
    )


def complete_feature(
    paths: HarnessPaths,
    feature_id: str,
    summary: str,
    validation: str | None = None,
) -> dict[str, Any]:
    data = load_features(paths)
    feature = _find_feature(data, feature_id)
    feature["state"] = "done"
    feature["updated_at"] = now_iso()
    feature["session_notes"].append({"at": now_iso(), "note": summary})
    save_features(paths, data)
    append_progress(paths, feature_id, feature["title"], summary, validation)
    refresh_session_brief(paths)
    return feature


def doctor(paths: HarnessPaths) -> list[str]:
    ensure_initialized(paths)
    issues: list[str] = []

    for required in (
        paths.config,
        paths.features,
        paths.feature_list,
        paths.progress,
        paths.claude_progress,
        paths.app_spec,
        paths.security,
        paths.initializer_prompt,
        paths.coding_prompt,
        paths.session_brief,
        paths.session_packet,
    ):
        if not required.exists():
            issues.append(f"Missing file: {required}")

    data = load_features(paths)
    seen_ids: set[str] = set()

    for feature in data["features"]:
        feature_id = feature.get("id")
        if not feature_id:
            issues.append("Feature missing id")
            continue
        if feature_id in seen_ids:
            issues.append(f"Duplicate feature id: {feature_id}")
        seen_ids.add(feature_id)

        state = feature.get("state")
        if state not in VALID_STATES:
            issues.append(f"Invalid state for {feature_id}: {state}")

        for dep in feature.get("depends_on", []):
            if dep not in seen_ids and dep not in {
                item["id"] for item in data["features"]
            }:
                issues.append(f"Unknown dependency for {feature_id}: {dep}")

    return issues


def status_summary(paths: HarnessPaths) -> dict[str, Any]:
    data = load_features(paths)
    counts = {state: 0 for state in VALID_STATES}
    for feature in data["features"]:
        counts[feature["state"]] += 1

    next_feature = select_next_feature(paths)
    return {
        "counts": counts,
        "next_feature": next_feature,
        "total": len(data["features"]),
    }


def build_agent_command(
    paths: HarnessPaths,
    prompt_kind: str,
    command_template: str,
) -> list[str]:
    prompt_path = paths.initializer_prompt if prompt_kind == "initializer" else paths.coding_prompt
    command = command_template.format(
        prompt=str(prompt_path),
        session_brief=str(paths.session_brief),
        session_packet=str(paths.session_packet),
        feature_list=str(paths.feature_list),
        app_spec=str(paths.app_spec),
    )
    return shlex.split(command, posix=False)


def run_agent_command(
    paths: HarnessPaths,
    prompt_kind: str,
    command_template: str,
    dry_run: bool = False,
) -> dict[str, Any]:
    command = build_agent_command(paths, prompt_kind, command_template)
    if dry_run:
        return {"ok": True, "dry_run": True, "command": command}

    result = subprocess.run(
        command,
        cwd=paths.root,
        capture_output=True,
        text=True,
        check=False,
    )
    return {
        "ok": result.returncode == 0,
        "returncode": result.returncode,
        "command": command,
        "stdout": result.stdout,
        "stderr": result.stderr,
    }
