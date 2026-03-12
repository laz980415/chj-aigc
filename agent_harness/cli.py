from __future__ import annotations

import argparse
import json
from pathlib import Path

from .core import (
    add_feature,
    complete_feature,
    doctor,
    init_harness,
    load_config,
    refresh_session_brief,
    resolve_paths,
    run_agent_command,
    select_next_feature,
    set_feature_state,
    status_summary,
    write_app_spec,
)


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Long-running agent harness")
    parser.add_argument(
        "--root",
        default=".",
        help="Project root for the harness",
    )

    subparsers = parser.add_subparsers(dest="command", required=True)

    init_parser = subparsers.add_parser("init", help="Initialize harness files")
    init_parser.add_argument("--project", required=True, help="Project name")

    import_parser = subparsers.add_parser(
        "import-spec",
        help="Import a requirements or product spec file into app_spec.md",
    )
    import_parser.add_argument(
        "--file",
        required=True,
        help="Path to a Markdown or text file containing the requirements",
    )

    add_parser = subparsers.add_parser("add-feature", help="Add a backlog feature")
    add_parser.add_argument("title", help="Feature title")
    add_parser.add_argument("--description", default="", help="Feature description")
    add_parser.add_argument(
        "--criteria",
        action="append",
        default=[],
        help="Acceptance criterion. Repeat for multiple items.",
    )
    add_parser.add_argument(
        "--depends-on",
        action="append",
        default=[],
        help="Feature id dependency. Repeat for multiple items.",
    )

    subparsers.add_parser("status", help="Show harness status")
    subparsers.add_parser("next", help="Show the next recommended feature")

    start_parser = subparsers.add_parser("start", help="Mark a feature in progress")
    start_parser.add_argument("feature_id", help="Feature id")
    start_parser.add_argument("--note", default=None, help="Optional session note")

    complete_parser = subparsers.add_parser("complete", help="Complete a feature")
    complete_parser.add_argument("feature_id", help="Feature id")
    complete_parser.add_argument("--summary", required=True, help="Work summary")
    complete_parser.add_argument("--validation", default=None, help="Validation note")

    block_parser = subparsers.add_parser("block", help="Mark a feature blocked")
    block_parser.add_argument("feature_id", help="Feature id")
    block_parser.add_argument("--note", required=True, help="Blocker note")

    subparsers.add_parser("doctor", help="Validate harness consistency")
    subparsers.add_parser("refresh", help="Regenerate session brief")

    run_parser = subparsers.add_parser(
        "run-agent",
        help="Build or run a command for an initializer or coding session",
    )
    run_parser.add_argument(
        "kind",
        choices=["initializer", "coding"],
        help="Prompt type to run",
    )
    run_parser.add_argument(
        "--command-template",
        required=True,
        help=(
            "Command template. Supports placeholders: {prompt}, {session_brief}, "
            "{session_packet}, {feature_list}, {app_spec}"
        ),
    )
    run_parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Only render the command without executing it",
    )

    plan_parser = subparsers.add_parser(
        "plan",
        help="Run the initializer agent to split the app spec into features",
    )
    plan_parser.add_argument(
        "--command-template",
        required=True,
        help=(
            "Command template. Supports placeholders: {prompt}, {session_brief}, "
            "{session_packet}, {feature_list}, {app_spec}"
        ),
    )
    plan_parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Only render the command without executing it",
    )

    work_parser = subparsers.add_parser(
        "work-next",
        help="Run the coding agent against the next recommended feature",
    )
    work_parser.add_argument(
        "--command-template",
        required=True,
        help=(
            "Command template. Supports placeholders: {prompt}, {session_brief}, "
            "{session_packet}, {feature_list}, {app_spec}"
        ),
    )
    work_parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Only render the command without executing it",
    )

    return parser


def _json_print(payload: object) -> None:
    print(json.dumps(payload, indent=2))


def main(argv: list[str] | None = None) -> int:
    parser = build_parser()
    args = parser.parse_args(argv)
    paths = resolve_paths(Path(args.root))

    if args.command == "init":
        paths = init_harness(paths.root, args.project)
        config = load_config(paths)
        _json_print({"ok": True, "project": config["project_name"], "root": str(paths.root)})
        return 0

    if args.command == "import-spec":
        spec_path = Path(args.file)
        if not spec_path.is_absolute():
            spec_path = (paths.root / spec_path).resolve()
        content = spec_path.read_text(encoding="utf-8")
        write_app_spec(paths, content)
        refresh_session_brief(paths)
        _json_print({"ok": True, "app_spec": str(paths.app_spec), "source": str(spec_path)})
        return 0

    if args.command == "add-feature":
        feature = add_feature(
            paths,
            title=args.title,
            description=args.description,
            acceptance_criteria=args.criteria,
            depends_on=args.depends_on,
        )
        _json_print(feature)
        return 0

    if args.command == "status":
        _json_print(status_summary(paths))
        return 0

    if args.command == "next":
        feature = select_next_feature(paths)
        if not feature:
            _json_print({"next_feature": None})
            return 0
        _json_print(feature)
        return 0

    if args.command == "start":
        feature = set_feature_state(paths, args.feature_id, "in_progress", args.note)
        _json_print(feature)
        return 0

    if args.command == "complete":
        feature = complete_feature(paths, args.feature_id, args.summary, args.validation)
        _json_print(feature)
        return 0

    if args.command == "block":
        feature = set_feature_state(paths, args.feature_id, "blocked", args.note)
        _json_print(feature)
        return 0

    if args.command == "doctor":
        issues = doctor(paths)
        _json_print({"ok": not issues, "issues": issues})
        return 0 if not issues else 1

    if args.command == "refresh":
        refresh_session_brief(paths)
        _json_print({"ok": True})
        return 0

    if args.command == "plan":
        payload = run_agent_command(
            paths,
            prompt_kind="initializer",
            command_template=args.command_template,
            dry_run=args.dry_run,
        )
        _json_print(payload)
        return 0 if payload.get("ok") else 1

    if args.command == "work-next":
        feature = select_next_feature(paths)
        if feature and feature["state"] == "pending":
            set_feature_state(paths, feature["id"], "in_progress", "Session started by work-next")
        payload = run_agent_command(
            paths,
            prompt_kind="coding",
            command_template=args.command_template,
            dry_run=args.dry_run,
        )
        _json_print(payload)
        return 0 if payload.get("ok") else 1

    if args.command == "run-agent":
        payload = run_agent_command(
            paths,
            prompt_kind=args.kind,
            command_template=args.command_template,
            dry_run=args.dry_run,
        )
        _json_print(payload)
        return 0 if payload.get("ok") else 1

    parser.error(f"Unknown command: {args.command}")
    return 2
