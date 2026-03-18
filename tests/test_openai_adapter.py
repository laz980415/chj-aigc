from __future__ import annotations

import sys
import unittest
from pathlib import Path
from unittest.mock import patch


ROOT = Path(__file__).resolve().parents[1]
MODEL_SERVICE_ROOT = ROOT / "backend-model-service"
if str(MODEL_SERVICE_ROOT) not in sys.path:
    sys.path.insert(0, str(MODEL_SERVICE_ROOT))

from platform_core.generation import GenerationResult, JobStatus
from platform_core.grounding import BrandContext, GenerationIntent
from platform_core.models import CapabilityType, seed_default_registry
from src.openai_adapter import OpenAIProviderAdapter
from src.provider_config import ProviderConfig


class RecordingFallback:
    def __init__(self) -> None:
        self.calls = 0

    def invoke(self, route, grounded_prompt) -> GenerationResult:
        self.calls += 1
        return GenerationResult(
            job_id="",
            status=JobStatus.SUCCEEDED,
            provider_id="fallback",
            provider_model_name="fake-model",
            output_text="fallback-result",
        )


def build_route(capability: CapabilityType, alias: str):
    registry = seed_default_registry()
    return registry.resolve_route(
        alias=alias,
        capability=capability,
        require_brand_grounding=True,
        require_async_jobs=capability == CapabilityType.VIDEO_GENERATION,
    )


def build_prompt(capability: CapabilityType):
    brand_context = BrandContext(
        tenant_id="tenant-demo",
        project_id="project-demo",
        client_name="Acme Group",
        brand_name="Acme Beauty",
        brand_summary="Premium skincare brand.",
    )
    intent = GenerationIntent(
        capability=capability,
        user_prompt="Write a launch headline." if capability == CapabilityType.COPYWRITING else "Generate a hero poster.",
    )
    return brand_context, intent


class OpenAIProviderAdapterTests(unittest.TestCase):
    def test_falls_back_when_provider_is_not_configured(self) -> None:
        route = build_route(CapabilityType.COPYWRITING, "copy-standard")
        brand_context, intent = build_prompt(CapabilityType.COPYWRITING)

        from platform_core.grounding import assemble_grounded_prompt

        grounded_prompt = assemble_grounded_prompt(brand_context, intent)
        fallback = RecordingFallback()
        adapter = OpenAIProviderAdapter(fallback=fallback)

        with patch("src.openai_adapter.get_provider", side_effect=RuntimeError("missing key")):
            result = adapter.invoke(route, grounded_prompt)

        self.assertEqual(fallback.calls, 1)
        self.assertEqual(result.provider_id, "fallback")
        self.assertEqual(result.output_text, "fallback-result")

    def test_uses_openai_chat_completion_when_provider_is_configured(self) -> None:
        route = build_route(CapabilityType.COPYWRITING, "copy-standard")
        brand_context, intent = build_prompt(CapabilityType.COPYWRITING)

        from platform_core.grounding import assemble_grounded_prompt

        grounded_prompt = assemble_grounded_prompt(brand_context, intent)
        adapter = OpenAIProviderAdapter(fallback=RecordingFallback())
        fake_config = ProviderConfig(
            api_key="test-key",
            api_base_url="https://example.test/v1",
            enabled=True,
        )

        with patch("src.openai_adapter.get_provider", return_value=fake_config):
            with patch.object(
                adapter,
                "_post_json",
                return_value={
                    "choices": [{"message": {"content": "品牌上新主标题"}}],
                    "usage": {"prompt_tokens": 33, "completion_tokens": 21},
                },
            ) as post_json:
                result = adapter.invoke(route, grounded_prompt)

        post_json.assert_called_once()
        self.assertEqual(result.status, JobStatus.SUCCEEDED)
        self.assertEqual(result.provider_id, "openai")
        self.assertEqual(result.output_text, "品牌上新主标题")
        self.assertEqual(result.input_tokens, 33)
        self.assertEqual(result.output_tokens, 21)


if __name__ == "__main__":
    unittest.main()
