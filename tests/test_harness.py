from __future__ import annotations

import json
import tempfile
import unittest
from pathlib import Path

from agent_harness.core import (
    add_feature,
    build_agent_command,
    complete_feature,
    doctor,
    init_harness,
    resolve_paths,
    run_agent_command,
    select_next_feature,
    status_summary,
    write_app_spec,
)


class HarnessTests(unittest.TestCase):
    def test_init_add_next_complete_cycle(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            paths = init_harness(tmp, "Demo")
            feature = add_feature(
                paths,
                "Build landing page",
                description="Create the first screen",
                acceptance_criteria=["Page renders", "Headline is visible"],
            )

            next_feature = select_next_feature(paths)
            self.assertIsNotNone(next_feature)
            self.assertEqual(next_feature["id"], feature["id"])

            complete_feature(
                paths,
                feature["id"],
                summary="Implemented the page",
                validation="Manual browser check",
            )

            summary = status_summary(paths)
            self.assertEqual(summary["counts"]["done"], 1)

    def test_doctor_detects_missing_dependency(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            paths = init_harness(tmp, "Demo")
            add_feature(paths, "Broken feature", depends_on=["feature-999"])

            issues = doctor(paths)
            self.assertTrue(any("Unknown dependency" in issue for issue in issues))

    def test_session_brief_file_is_created(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            init_harness(tmp, "Demo")
            paths = resolve_paths(Path(tmp))
            self.assertTrue(paths.session_brief.exists())
            self.assertTrue(paths.feature_list.exists())
            self.assertTrue(paths.claude_progress.exists())
            content = paths.session_brief.read_text(encoding="utf-8")
            self.assertIn("Session Brief", content)

    def test_run_agent_command_renders_placeholders(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            paths = init_harness(tmp, "Demo")
            command = build_agent_command(
                paths,
                "coding",
                'claude -p "@{prompt}" --brief "@{session_brief}"',
            )
            rendered = " ".join(command)
            self.assertIn("coding_prompt.md", rendered)
            self.assertIn("session_brief.md", rendered)

    def test_write_app_spec_and_dry_run_agent(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            paths = init_harness(tmp, "Demo")
            write_app_spec(paths, "# Demo\n\nBuild a todo app.")
            self.assertIn("todo app", paths.app_spec.read_text(encoding="utf-8"))

            payload = run_agent_command(
                paths,
                prompt_kind="initializer",
                command_template='claude -p "@{prompt}" --spec "@{app_spec}"',
                dry_run=True,
            )
            self.assertTrue(payload["ok"])
            rendered = " ".join(payload["command"])
            self.assertIn("app_spec.md", rendered)


if __name__ == "__main__":
    unittest.main()
