from __future__ import annotations

import unittest

from platform_core.grounding import (
    AssetKind,
    BrandContext,
    BrandRuleKind,
    GenerationIntent,
    GroundingAsset,
    GroundingRule,
    assemble_grounded_prompt,
)
from platform_core.models import CapabilityType


class GroundingTests(unittest.TestCase):
    def test_prompt_includes_brand_rules_and_assets(self) -> None:
        brand_context = BrandContext(
            tenant_id="tenant-1",
            project_id="project-1",
            client_name="Acme Group",
            brand_name="Acme Beauty",
            brand_summary="Premium skincare for urban professionals.",
            rules=(
                GroundingRule("rule-1", BrandRuleKind.TONE, "Confident and refined."),
                GroundingRule("rule-2", BrandRuleKind.FORBIDDEN_STATEMENT, "Do not claim medical cures."),
                GroundingRule("rule-3", BrandRuleKind.REQUIRED_STATEMENT, "Highlight hydration benefits."),
            ),
            assets=(
                GroundingAsset(
                    id="asset-1",
                    name="Hero bottle shot",
                    kind=AssetKind.IMAGE,
                    uri="oss://assets/hero-bottle.png",
                    tags=("hero", "product"),
                    summary="Front-facing packshot on white background.",
                ),
            ),
        )
        intent = GenerationIntent(
            capability=CapabilityType.COPYWRITING,
            user_prompt="Write a short ad headline and body copy.",
            audience="Women age 25-35",
            channel="Instagram",
            objective="Promote the new serum launch",
            output_format="headline plus two supporting lines",
        )

        prompt = assemble_grounded_prompt(brand_context, intent)

        self.assertIn("Acme Beauty", prompt.system_prompt)
        self.assertIn("Confident and refined.", prompt.system_prompt)
        self.assertIn("Do not claim medical cures.", prompt.system_prompt)
        self.assertIn("Hero bottle shot", prompt.system_prompt)
        self.assertIn("Objective: Promote the new serum launch", prompt.user_prompt)
        self.assertEqual(prompt.audit.selected_rule_ids, ("rule-1", "rule-2", "rule-3"))
        self.assertEqual(prompt.audit.selected_asset_ids, ("asset-1",))

    def test_prompt_limits_assets_for_traceability(self) -> None:
        brand_context = BrandContext(
            tenant_id="tenant-1",
            project_id="project-1",
            client_name="Acme Group",
            brand_name="Acme Beauty",
            brand_summary="Premium skincare.",
            assets=tuple(
                GroundingAsset(
                    id=f"asset-{index}",
                    name=f"Asset {index}",
                    kind=AssetKind.IMAGE,
                    uri=f"oss://assets/{index}.png",
                )
                for index in range(7)
            ),
        )
        intent = GenerationIntent(
            capability=CapabilityType.IMAGE_GENERATION,
            user_prompt="Generate a campaign visual.",
        )

        prompt = assemble_grounded_prompt(brand_context, intent, max_assets=3)

        self.assertEqual(len(prompt.audit.selected_asset_ids), 3)
        self.assertIn("3 referenced assets", prompt.context_summary)


if __name__ == "__main__":
    unittest.main()
