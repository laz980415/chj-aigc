from __future__ import annotations

from decimal import Decimal
import unittest

from platform_core.billing import ChargeResult, PriceUnit
from platform_core.generation import GenerationRequest, GenerationResult, JobStatus
from platform_core.grounding import BrandContext, GenerationIntent
from platform_core.models import CapabilityType
from platform_core.observability import SafetyPolicy, build_generation_audit_record


class ObservabilityTests(unittest.TestCase):
    def test_generation_audit_record_contains_summaries_and_charge(self) -> None:
        request = GenerationRequest(
            job_id="job-1",
            tenant_id="tenant-1",
            project_id="project-1",
            actor_id="user-1",
            model_alias="copy-standard",
            intent=GenerationIntent(
                capability=CapabilityType.COPYWRITING,
                user_prompt="Write an ad headline for a new serum launch.",
            ),
            brand_context=BrandContext(
                tenant_id="tenant-1",
                project_id="project-1",
                client_name="Client A",
                brand_name="Brand A",
                brand_summary="Premium brand",
            ),
        )
        result = GenerationResult(
            job_id="job-1",
            status=JobStatus.SUCCEEDED,
            provider_id="openai",
            provider_model_name="gpt-4.1",
            output_text="Launch radiant skin today.",
        )
        charge = ChargeResult(
            rule_id="price-1",
            price_unit=PriceUnit.CREDITS,
            total=Decimal("1.2500"),
            breakdown=(),
        )

        record = build_generation_audit_record(request, result, charge)

        self.assertEqual(record.job_id, "job-1")
        self.assertEqual(record.provider_id, "openai")
        self.assertEqual(record.charge_total, "1.2500")
        self.assertIn("serum launch", record.request_summary)

    def test_safety_policy_violation_is_captured(self) -> None:
        request = GenerationRequest(
            job_id="job-2",
            tenant_id="tenant-1",
            project_id="project-1",
            actor_id="user-2",
            model_alias="copy-standard",
            intent=GenerationIntent(
                capability=CapabilityType.COPYWRITING,
                user_prompt="Write promotional copy.",
            ),
            brand_context=BrandContext(
                tenant_id="tenant-1",
                project_id="project-1",
                client_name="Client A",
                brand_name="Brand A",
                brand_summary="Premium brand",
            ),
        )
        result = GenerationResult(
            job_id="job-2",
            status=JobStatus.SUCCEEDED,
            provider_id="openai",
            provider_model_name="gpt-4.1",
            output_text="This serum can cure acne permanently.",
        )
        charge = ChargeResult(
            rule_id="price-1",
            price_unit=PriceUnit.CREDITS,
            total=Decimal("0.5000"),
            breakdown=(),
        )
        policies = (
            SafetyPolicy(
                id="policy-1",
                name="Medical claims policy",
                forbidden_terms=("cure acne permanently",),
            ),
        )

        record = build_generation_audit_record(request, result, charge, policies)

        self.assertEqual(len(record.safety_violations), 1)
        self.assertEqual(record.safety_violations[0].policy_id, "policy-1")


if __name__ == "__main__":
    unittest.main()
