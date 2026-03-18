from __future__ import annotations

import sys
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
MODEL_SERVICE_ROOT = ROOT / "backend-model-service"
if str(MODEL_SERVICE_ROOT) not in sys.path:
    sys.path.insert(0, str(MODEL_SERVICE_ROOT))

from src.asset_grounding import AssetGroundingService, GroundingAssetRef
from src.asset_ingestion import AssetContextRetriever, AssetDescriptor, InMemoryAssetChunkIndex, build_default_asset_ingestion_service, DeterministicEmbeddingProvider


class AssetGroundingTests(unittest.TestCase):
    def setUp(self) -> None:
        self.workspace = ROOT / "tmp-asset-grounding-tests"
        self.workspace.mkdir(exist_ok=True)

    def tearDown(self) -> None:
        for path in self.workspace.glob("*"):
            path.unlink()
        self.workspace.rmdir()

    def test_grounding_context_returns_relevant_snippets_for_selected_assets(self) -> None:
        index = InMemoryAssetChunkIndex()
        ingestion_service = build_default_asset_ingestion_service()
        ingestion_service.chunk_index = index
        retriever = AssetContextRetriever(DeterministicEmbeddingProvider(), index)
        grounding_service = AssetGroundingService(retriever)

        first = self.workspace / "brand-guide.txt"
        second = self.workspace / "other.txt"
        first.write_text("Hydration serum campaign with premium skincare tone", encoding="utf-8")
        second.write_text("Tea beverage summer campaign", encoding="utf-8")

        ingestion_service.ingest(
            AssetDescriptor(
                asset_id="asset-guide",
                tenant_id="tenant-demo",
                project_id="project-demo",
                brand_id="brand-demo",
                name="品牌手册",
                kind="document",
                uri=str(first),
            )
        )
        ingestion_service.ingest(
            AssetDescriptor(
                asset_id="asset-other",
                tenant_id="tenant-demo",
                project_id="project-demo",
                brand_id="brand-demo",
                name="其他文档",
                kind="document",
                uri=str(second),
            )
        )

        context = grounding_service.build_context(
            tenant_id="tenant-demo",
            brand_id="brand-demo",
            user_prompt="write premium hydration skincare copy",
            assets=(
                GroundingAssetRef(
                    asset_id="asset-guide",
                    name="品牌手册",
                    kind="document",
                    uri=str(first),
                ),
            ),
            limit=3,
        )

        self.assertEqual(len(context.snippets), 1)
        self.assertEqual(context.snippets[0].asset_id, "asset-guide")
        self.assertIn("Retrieved 1 semantic asset chunks", context.context_summary)


if __name__ == "__main__":
    unittest.main()
