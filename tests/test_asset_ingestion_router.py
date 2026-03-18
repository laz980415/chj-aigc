from __future__ import annotations

import sys
import unittest
from pathlib import Path

from fastapi.testclient import TestClient


ROOT = Path(__file__).resolve().parents[1]
MODEL_SERVICE_ROOT = ROOT / "backend-model-service"
if str(MODEL_SERVICE_ROOT) not in sys.path:
    sys.path.insert(0, str(MODEL_SERVICE_ROOT))

from src.main import app
from src.routers import assets as assets_router


class AssetIngestionRouterTests(unittest.TestCase):
    def setUp(self) -> None:
        self.workspace = ROOT / "tmp-asset-router-tests"
        self.workspace.mkdir(exist_ok=True)
        if assets_router._index_path.exists():
            assets_router._index_path.unlink()
        self.client = TestClient(app)

    def tearDown(self) -> None:
        if assets_router._index_path.exists():
            assets_router._index_path.unlink()
        for path in self.workspace.glob("*"):
            path.unlink()
        self.workspace.rmdir()

    def test_asset_ingest_endpoint_indexes_chunks(self) -> None:
        document_path = self.workspace / "guide.txt"
        document_path.write_text(
            "Hydration story for urban skincare.\n\n"
            "Avoid medical claim wording.\n\n"
            "Keep the tone premium and calm.",
            encoding="utf-8",
        )

        response = self.client.post(
            "/api/model/assets/ingest",
            json={
                "asset_id": "asset-router-1",
                "tenant_id": "tenant-demo",
                "project_id": "project-demo",
                "brand_id": "brand-demo",
                "name": "品牌文档",
                "kind": "document",
                "uri": str(document_path),
                "tags": ["guide"],
                "summary": "品牌约束文档",
            },
        )

        self.assertEqual(response.status_code, 200)
        payload = response.json()
        self.assertEqual(payload["asset_id"], "asset-router-1")
        self.assertGreaterEqual(payload["chunk_count"], 1)
        self.assertEqual(payload["chunks"][0]["brand_id"], "brand-demo")

    def test_asset_search_endpoint_filters_by_tenant_and_brand(self) -> None:
        first = self.workspace / "first.txt"
        second = self.workspace / "second.txt"
        first.write_text("Hydration skincare premium serum", encoding="utf-8")
        second.write_text("Summer tea beverage poster", encoding="utf-8")

        self.client.post(
            "/api/model/assets/ingest",
            json={
                "asset_id": "asset-search-1",
                "tenant_id": "tenant-demo",
                "project_id": "project-demo",
                "brand_id": "brand-a",
                "name": "护肤文档",
                "kind": "document",
                "uri": str(first),
            },
        )
        self.client.post(
            "/api/model/assets/ingest",
            json={
                "asset_id": "asset-search-2",
                "tenant_id": "tenant-demo",
                "project_id": "project-demo",
                "brand_id": "brand-b",
                "name": "饮料文档",
                "kind": "document",
                "uri": str(second),
            },
        )

        response = self.client.post(
            "/api/model/assets/search",
            json={
                "query": "premium skincare hydration",
                "tenant_id": "tenant-demo",
                "brand_id": "brand-a",
                "limit": 3,
            },
        )

        self.assertEqual(response.status_code, 200)
        payload = response.json()
        self.assertEqual(payload["total"], 1)
        self.assertEqual(payload["items"][0]["asset_id"], "asset-search-1")

    def test_grounding_context_endpoint_returns_selected_asset_snippets(self) -> None:
        document = self.workspace / "grounding.txt"
        document.write_text("Hydration skincare premium serum tone", encoding="utf-8")

        self.client.post(
            "/api/model/assets/ingest",
            json={
                "asset_id": "asset-grounding-1",
                "tenant_id": "tenant-demo",
                "project_id": "project-demo",
                "brand_id": "brand-demo",
                "name": "品牌手册",
                "kind": "document",
                "uri": str(document),
            },
        )

        response = self.client.post(
            "/api/model/assets/grounding-context",
            json={
                "tenant_id": "tenant-demo",
                "brand_id": "brand-demo",
                "user_prompt": "write premium hydration skincare copy",
                "assets": [
                    {
                        "asset_id": "asset-grounding-1",
                        "name": "品牌手册",
                        "kind": "document",
                        "uri": str(document),
                    }
                ],
            },
        )

        self.assertEqual(response.status_code, 200)
        payload = response.json()
        self.assertEqual(len(payload["snippets"]), 1)
        self.assertEqual(payload["snippets"][0]["asset_id"], "asset-grounding-1")


if __name__ == "__main__":
    unittest.main()
