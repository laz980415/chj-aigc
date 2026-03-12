from __future__ import annotations

from textwrap import dedent


def initializer_prompt(project_name: str) -> str:
    return dedent(
        f"""\
        # Initializer Prompt

        You are the initializer agent for "{project_name}".

        Your task is to turn the project specification into a durable, incremental execution plan.

        Read first:
        - .agent-harness/app_spec.md
        - .agent-harness/security.json

        Produce and maintain:
        - .agent-harness/feature_list.json
        - .agent-harness/claude-progress.md
        - .agent-harness/session_brief.md

        Requirements:
        - Break the project into small, testable features.
        - Prefer concrete acceptance criteria.
        - Record dependencies explicitly.
        - Leave enough context that a fresh coding agent can resume work.
        - Never declare the project complete until the feature list is actually complete.
        """
    )


def coding_prompt(project_name: str) -> str:
    return dedent(
        f"""\
        # Coding Prompt

        You are the coding agent for "{project_name}".

        You are operating inside a long-running agent harness. Work incrementally and leave durable state.

        Read first:
        - .agent-harness/session_brief.md
        - .agent-harness/feature_list.json
        - .agent-harness/claude-progress.md
        - .agent-harness/security.json

        Rules:
        - Work on one feature per session unless blocked.
        - Validate changes before claiming success.
        - Update the feature list and progress log before ending.
        - If blocked, record the blocker precisely.
        - Do not hide failed attempts or partial progress.

        Before ending the session:
        - Update .agent-harness/feature_list.json
        - Append a concise entry to .agent-harness/claude-progress.md
        - Refresh .agent-harness/session_brief.md
        """
    )


def default_app_spec(project_name: str) -> str:
    return dedent(
        f"""\
        # {project_name}

        ## Product Goal
        Describe the product or system this harness should build.

        ## Users
        List the target users and their main workflows.

        ## Scope
        Define the in-scope capabilities for the first iteration.

        ## Constraints
        Record technical, product, security, or delivery constraints.

        ## Definition of Done
        Describe what must be true before the project can be considered complete.
        """
    )


def default_security_policy() -> str:
    return dedent(
        """\
        {
          "allowed_paths": [
            "."
          ],
          "blocked_paths": [
            ".env",
            "secrets",
            "credentials"
          ],
          "allowed_commands": [
            "python",
            "pytest",
            "npm",
            "node",
            "git status",
            "git diff"
          ],
          "notes": [
            "Review destructive commands manually.",
            "Add project-specific restrictions before autonomous execution."
          ]
        }
        """
    )


def session_packet(project_name: str, next_feature_id: str | None) -> str:
    feature_line = next_feature_id or "No unblocked feature selected yet"
    return dedent(
        f"""\
        # Session Packet

        Project: {project_name}
        Recommended feature: {feature_line}

        Execution order:
        1. Read .agent-harness/session_brief.md
        2. Implement the next recommended feature
        3. Validate the result
        4. Update feature_list.json and claude-progress.md
        5. Refresh session_brief.md before ending
        """
    )
