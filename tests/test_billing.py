from __future__ import annotations

from decimal import Decimal
import unittest

from platform_core.billing import (
    Ledger,
    MeteredUsage,
    PriceUnit,
    VendorReference,
    seed_default_pricing,
)
from platform_core.models import MeteringDimension


class BillingTests(unittest.TestCase):
    def test_copy_quote_uses_token_components(self) -> None:
        pricing = seed_default_pricing()

        result = pricing.quote(
            platform_model_id="pm-copy-standard",
            capability_key="copywriting",
            usage=[
                MeteredUsage(MeteringDimension.INPUT_TOKENS, Decimal("1500")),
                MeteredUsage(MeteringDimension.OUTPUT_TOKENS, Decimal("500")),
            ],
        )

        self.assertEqual(result.price_unit, PriceUnit.CREDITS)
        self.assertEqual(result.total, Decimal("0.0050"))
        self.assertEqual(len(result.breakdown), 2)

    def test_ledger_tracks_recharge_and_usage_balance(self) -> None:
        pricing = seed_default_pricing()
        ledger = Ledger()
        ledger.record_recharge(
            entry_id="recharge-1",
            tenant_id="tenant-1",
            amount=Decimal("100.0000"),
            price_unit=PriceUnit.CREDITS,
            description="Manual recharge",
            reference_id="order-1",
        )

        quote = pricing.quote(
            platform_model_id="pm-image-standard",
            capability_key="image_generation",
            usage=[MeteredUsage(MeteringDimension.IMAGE_COUNT, Decimal("3"))],
        )
        usage_entry = ledger.bill_usage(
            entry_id="usage-1",
            tenant_id="tenant-1",
            description="Generated 3 images",
            reference_id="task-1",
            reference_type="generation_task",
            model_id="pm-image-standard",
            vendor_reference=VendorReference(
                provider_id="stability",
                provider_model_name="stable-image-ultra",
                raw_cost=Decimal("4.2000"),
                raw_currency="USD",
            ),
            charge_result=quote,
        )

        self.assertEqual(usage_entry.amount, Decimal("-7.5000"))
        self.assertEqual(ledger.balance("tenant-1", PriceUnit.CREDITS), Decimal("92.5000"))
        self.assertEqual(usage_entry.vendor_reference.provider_id, "stability")

    def test_historical_ledger_snapshot_is_stable_after_price_change(self) -> None:
        pricing = seed_default_pricing()
        quote_before = pricing.quote(
            platform_model_id="pm-video-standard",
            capability_key="video_generation",
            usage=[MeteredUsage(MeteringDimension.VIDEO_SECONDS, Decimal("5"))],
        )

        pricing.rules[2] = pricing.rules[2].__class__(
            id="price-video-standard-v2",
            platform_model_id="pm-video-standard",
            capability_key="video_generation",
            price_unit=PriceUnit.CREDITS,
            components=pricing.rules[2].components.__class__(
                [
                    pricing.rules[2].components[0].__class__(
                        dimension=MeteringDimension.VIDEO_SECONDS,
                        unit_price=Decimal("8.0000"),
                        unit_size=Decimal("1"),
                    )
                ]
            ),
            description="Updated video price",
        )

        quote_after = pricing.quote(
            platform_model_id="pm-video-standard",
            capability_key="video_generation",
            usage=[MeteredUsage(MeteringDimension.VIDEO_SECONDS, Decimal("5"))],
        )

        self.assertEqual(quote_before.total, Decimal("20.0000"))
        self.assertEqual(quote_after.total, Decimal("40.0000"))


if __name__ == "__main__":
    unittest.main()
