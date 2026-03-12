from __future__ import annotations

import unittest

from platform_core.generation import (
    FakeProviderAdapter,
    GenerationOrchestrator,
    GenerationRequest,
    JobStatus,
)
from platform_core.grounding import (
    AssetKind,
    BrandContext,
    BrandRuleKind,
    GenerationIntent,
    GroundingAsset,
    GroundingRule,
)
from platform_core.models import CapabilityType, seed_default_registry


def sample_brand_context() -> BrandContext:
    return BrandContext(
        tenant_id="tenant-1",
        project_id="project-1",
        client_name="Acme Group",
        brand_name="Acme Beauty",
        brand_summary="Premium skincare for urban professionals.",
        rules=(
            GroundingRule("rule-1", BrandRuleKind.TONE, "Confident and refined."),
            GroundingRule("rule-2", BrandRuleKind.FORBIDDEN_STATEMENT, "Do not claim medical cures."),
        ),
        assets=(
            GroundingAsset(
                id="asset-1",
                name="Hero bottle shot",
                kind=AssetKind.IMAGE,
                uri="oss://assets/hero-bottle.png",
                summary="Front-facing packshot.",
            ),
        ),
    )


class GenerationOrchestratorTests(unittest.TestCase):
    def setUp(self) -> None:
        self.registry = seed_default_registry()
        self.orchestrator = GenerationOrchestrator(
            registry=self.registry,
            adapter=FakeProviderAdapter(),
        )

    def test_copy_generation_dispatches_synchronously(self) -> None:
        request = GenerationRequest(
            job_id="job-copy-1",
            tenant_id="tenant-1",
            project_id="project-1",
            actor_id="user-1",
            model_alias="copy-standard",
            intent=GenerationIntent(
                capability=CapabilityType.COPYWRITING,
                user_prompt="Write launch copy for the new serum.",
            ),
            brand_context=sample_brand_context(),
        )

        result = self.orchestrator.dispatch(request)

        self.assertEqual(result.status, JobStatus.SUCCEEDED)
        self.assertIn("Acme Beauty", result.output_text)

    def test_image_generation_returns_output_uri(self) -> None:
        request = GenerationRequest(
            job_id="job-image-1",
            tenant_id="tenant-1",
            project_id="project-1",
            actor_id="user-1",
            model_alias="image-standard",
            intent=GenerationIntent(
                capability=CapabilityType.IMAGE_GENERATION,
                user_prompt="Generate a premium product poster.",
            ),
            brand_context=sample_brand_context(),
        )

        result = self.orchestrator.dispatch(request)

        self.assertEqual(result.status, JobStatus.SUCCEEDED)
        self.assertTrue(result.output_uri.startswith("oss://generated/image-standard"))

    def test_video_generation_returns_pending_and_can_complete_async(self) -> None:
        request = GenerationRequest(
            job_id="job-video-1",
            tenant_id="tenant-1",
            project_id="project-1",
            actor_id="user-1",
            model_alias="video-standard",
            intent=GenerationIntent(
                capability=CapabilityType.VIDEO_GENERATION,
                user_prompt="Create a 10-second launch teaser.",
            ),
            brand_context=sample_brand_context(),
        )

        pending = self.orchestrator.dispatch(request)

        self.assertEqual(pending.status, JobStatus.PENDING)
        self.assertTrue(pending.provider_job_id.startswith("provider-job-"))

        completed = self.orchestrator.mark_async_complete(
            "job-video-1",
            "oss://generated/video-standard/teaser.mp4",
        )

        self.assertEqual(completed.status, JobStatus.SUCCEEDED)
        self.assertEqual(completed.output_uri, "oss://generated/video-standard/teaser.mp4")

    def test_provider_invocation_contains_grounded_prompts(self) -> None:
        request = GenerationRequest(
            job_id="job-copy-2",
            tenant_id="tenant-1",
            project_id="project-1",
            actor_id="user-2",
            model_alias="copy-standard",
            intent=GenerationIntent(
                capability=CapabilityType.COPYWRITING,
                user_prompt="Write a social caption.",
            ),
            brand_context=sample_brand_context(),
        )

        invocation = self.orchestrator.build_provider_invocation(request)

        self.assertEqual(invocation.provider_id, "openai")
        self.assertIn("Do not claim medical cures.", invocation.system_prompt)
        self.assertIn("Write a social caption.", invocation.user_prompt)


if __name__ == "__main__":
    unittest.main()
