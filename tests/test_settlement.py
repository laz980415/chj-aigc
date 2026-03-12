from __future__ import annotations

from decimal import Decimal
import unittest

from platform_core.billing import Ledger, PriceUnit, seed_default_pricing
from platform_core.generation import GenerationRequest, GenerationResult, JobStatus
from platform_core.grounding import BrandContext, GenerationIntent
from platform_core.models import CapabilityType, seed_default_registry
from platform_core.settlement import (
    QuotaAllocation,
    QuotaBook,
    QuotaDimension,
    QuotaScope,
    QuotaScopeType,
    SettlementCoordinator,
)


def empty_brand_context() -> BrandContext:
    return BrandContext(
        tenant_id="tenant-1",
        project_id="project-1",
        client_name="Client A",
        brand_name="Brand A",
        brand_summary="Brand summary",
    )


class SettlementTests(unittest.TestCase):
    def setUp(self) -> None:
        self.registry = seed_default_registry()
        self.pricing = seed_default_pricing()
        self.ledger = Ledger()
        self.ledger.record_recharge(
            entry_id="recharge-1",
            tenant_id="tenant-1",
            amount=Decimal("500.0000"),
            price_unit=PriceUnit.CREDITS,
            description="seed balance",
            reference_id="manual-1",
        )
        self.quota_book = QuotaBook()
        for scope in (
            QuotaScope(QuotaScopeType.USER, "user-1"),
            QuotaScope(QuotaScopeType.PROJECT, "project-1"),
        ):
            self.quota_book.upsert(QuotaAllocation(scope, QuotaDimension.TOKENS, Decimal("10000")))
            self.quota_book.upsert(QuotaAllocation(scope, QuotaDimension.IMAGE_COUNT, Decimal("20")))
            self.quota_book.upsert(QuotaAllocation(scope, QuotaDimension.VIDEO_SECONDS, Decimal("60")))
            self.quota_book.upsert(QuotaAllocation(scope, QuotaDimension.DAILY_REQUESTS, Decimal("10")))

        self.coordinator = SettlementCoordinator(
            registry=self.registry,
            pricing=self.pricing,
            ledger=self.ledger,
            quota_book=self.quota_book,
        )

    def test_copy_settlement_deducts_balance_and_token_quotas(self) -> None:
        request = GenerationRequest(
            job_id="job-copy-1",
            tenant_id="tenant-1",
            project_id="project-1",
            actor_id="user-1",
            model_alias="copy-standard",
            intent=GenerationIntent(
                capability=CapabilityType.COPYWRITING,
                user_prompt="Write a launch headline and short body copy.",
            ),
            brand_context=empty_brand_context(),
        )
        result = GenerationResult(
            job_id="job-copy-1",
            status=JobStatus.SUCCEEDED,
            provider_id="openai",
            provider_model_name="gpt-4.1",
            output_text="Launch brighter skin with confidence.",
        )

        settlement = self.coordinator.settle(request=request, result=result, entry_id="usage-1")

        self.assertGreater(settlement.charge_result.total, Decimal("0"))
        self.assertEqual(self.ledger.balance("tenant-1", PriceUnit.CREDITS), Decimal("499.9995"))
        self.assertLess(
            self.quota_book.find(QuotaScope(QuotaScopeType.USER, "user-1"), QuotaDimension.TOKENS).remaining,
            Decimal("10000"),
        )

    def test_image_settlement_consumes_image_quota(self) -> None:
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
            brand_context=empty_brand_context(),
        )
        result = GenerationResult(
            job_id="job-image-1",
            status=JobStatus.SUCCEEDED,
            provider_id="stability",
            provider_model_name="stable-image-ultra",
            output_uri="oss://generated/image-standard/1.png",
        )

        settlement = self.coordinator.settle(request=request, result=result, entry_id="usage-2")

        self.assertEqual(settlement.charge_result.total, Decimal("2.5000"))
        self.assertEqual(
            self.quota_book.find(QuotaScope(QuotaScopeType.PROJECT, "project-1"), QuotaDimension.IMAGE_COUNT).remaining,
            Decimal("19"),
        )

    def test_quota_violation_blocks_settlement(self) -> None:
        self.quota_book.upsert(
            QuotaAllocation(
                QuotaScope(QuotaScopeType.USER, "user-1"),
                QuotaDimension.VIDEO_SECONDS,
                Decimal("5"),
            )
        )
        request = GenerationRequest(
            job_id="job-video-1",
            tenant_id="tenant-1",
            project_id="project-1",
            actor_id="user-1",
            model_alias="video-standard",
            intent=GenerationIntent(
                capability=CapabilityType.VIDEO_GENERATION,
                user_prompt="Generate a teaser video.",
            ),
            brand_context=empty_brand_context(),
        )
        result = GenerationResult(
            job_id="job-video-1",
            status=JobStatus.PENDING,
            provider_id="runway",
            provider_model_name="gen4_turbo",
            provider_job_id="provider-job-1",
        )

        with self.assertRaises(ValueError):
            self.coordinator.settle(request=request, result=result, entry_id="usage-3")


if __name__ == "__main__":
    unittest.main()
