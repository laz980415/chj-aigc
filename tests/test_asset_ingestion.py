from __future__ import annotations

import sys
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
MODEL_SERVICE_ROOT = ROOT / "backend-model-service"
if str(MODEL_SERVICE_ROOT) not in sys.path:
    sys.path.insert(0, str(MODEL_SERVICE_ROOT))

from src.asset_ingestion import (
    AssetContextRetriever,
    AssetDescriptor,
    DeterministicEmbeddingProvider,
    DocumentAssetParser,
    ElasticsearchAssetChunkIndex,
    ImageAssetParser,
    InMemoryAssetChunkIndex,
    JsonlAssetChunkIndex,
    VideoAssetParser,
    build_default_asset_ingestion_service,
)
from unittest.mock import MagicMock, patch


class AssetIngestionTests(unittest.TestCase):
    def setUp(self) -> None:
        self.workspace = ROOT / "tmp-asset-tests"
        self.workspace.mkdir(exist_ok=True)

    def tearDown(self) -> None:
        for path in self.workspace.glob("*"):
            path.unlink()
        self.workspace.rmdir()

    def test_document_assets_are_chunked_and_indexed(self) -> None:
        document_path = self.workspace / "brand-guide.txt"
        document_path.write_text(
            "Hydration is the core product story for the new serum launch, and every campaign should reinforce premium efficacy cues without sounding clinical.\n\n"
            "Avoid medical cure claims in every campaign, avoid pharmaceutical wording, and keep the language focused on texture, comfort, confidence, and daily ritual.\n\n"
            "Keep the tone premium and calm, highlight white background packshots, emphasize urban skincare routines, and mention product texture when describing visuals.",
            encoding="utf-8",
        )
        service = build_default_asset_ingestion_service()
        asset = AssetDescriptor(
            asset_id="asset-doc-1",
            tenant_id="tenant-demo",
            project_id="project-demo",
            brand_id="brand-demo",
            name="品牌手册",
            kind="document",
            uri=str(document_path),
            tags=("guide", "hydration"),
            summary="品牌文字手册",
        )

        chunks = service.ingest(asset)

        self.assertGreaterEqual(len(chunks), 2)
        self.assertTrue(all(chunk.page_no is not None for chunk in chunks))
        self.assertTrue(all(len(chunk.embedding) == 16 for chunk in chunks))

    def test_image_and_video_assets_generate_descriptive_chunks(self) -> None:
        image_path = self.workspace / "hero.png"
        image_path.write_bytes(b"fake-image")
        video_path = self.workspace / "teaser.mp4"
        video_path.write_bytes(b"fake-video")

        image_chunks = ImageAssetParser().parse(
            AssetDescriptor(
                asset_id="asset-image-1",
                tenant_id="tenant-demo",
                project_id="project-demo",
                brand_id="brand-demo",
                name="主视觉",
                kind="image",
                uri=str(image_path),
                tags=("hero", "launch"),
                summary="白底产品图",
            )
        )
        video_chunks = VideoAssetParser().parse(
            AssetDescriptor(
                asset_id="asset-video-1",
                tenant_id="tenant-demo",
                project_id="project-demo",
                brand_id="brand-demo",
                name="短视频",
                kind="video",
                uri=str(video_path),
                tags=("teaser",),
                summary="新品上市预热视频",
            )
        )

        self.assertEqual(len(image_chunks), 1)
        self.assertIn("主视觉", image_chunks[0].content_text)
        self.assertEqual(len(video_chunks), 3)
        self.assertEqual(video_chunks[0].frame_no, 1)
        self.assertIn("关键帧 3", video_chunks[2].summary)

    def test_semantic_retrieval_respects_tenant_and_brand_filters(self) -> None:
        first_path = self.workspace / "asset-1.txt"
        second_path = self.workspace / "asset-2.txt"
        third_path = self.workspace / "asset-3.txt"
        first_path.write_text("Hydration serum launch premium skincare", encoding="utf-8")
        second_path.write_text("Tea beverage campaign bright summer poster", encoding="utf-8")
        third_path.write_text("Hydration serum launch premium skincare", encoding="utf-8")

        index = InMemoryAssetChunkIndex()
        service = build_default_asset_ingestion_service()
        service.chunk_index = index
        retriever = AssetContextRetriever(DeterministicEmbeddingProvider(), index)

        service.ingest(
            AssetDescriptor(
                asset_id="asset-1",
                tenant_id="tenant-a",
                project_id="project-demo",
                brand_id="brand-a",
                name="护肤文档",
                kind="document",
                uri=str(first_path),
            )
        )
        service.ingest(
            AssetDescriptor(
                asset_id="asset-2",
                tenant_id="tenant-a",
                project_id="project-demo",
                brand_id="brand-b",
                name="饮料文档",
                kind="document",
                uri=str(second_path),
            )
        )
        service.ingest(
            AssetDescriptor(
                asset_id="asset-3",
                tenant_id="tenant-b",
                project_id="project-demo",
                brand_id="brand-a",
                name="其他租户文档",
                kind="document",
                uri=str(third_path),
            )
        )

        results = retriever.retrieve(
            "premium hydration skincare",
            tenant_id="tenant-a",
            brand_id="brand-a",
            limit=3,
        )

        self.assertEqual(len(results), 1)
        self.assertEqual(results[0].asset_id, "asset-1")

    def test_jsonl_index_persists_chunks(self) -> None:
        document_path = self.workspace / "notes.txt"
        document_path.write_text("Brand hydration note", encoding="utf-8")
        index_path = self.workspace / "asset-chunks.jsonl"
        service = build_default_asset_ingestion_service(index_path=index_path)
        asset = AssetDescriptor(
            asset_id="asset-jsonl-1",
            tenant_id="tenant-demo",
            project_id="project-demo",
            brand_id="brand-demo",
            name="说明",
            kind="document",
            uri=str(document_path),
        )

        chunks = service.ingest(asset)
        persisted = JsonlAssetChunkIndex(index_path).search(
            DeterministicEmbeddingProvider().embed("hydration note"),
            tenant_id="tenant-demo",
        )

        self.assertTrue(index_path.exists())
        self.assertEqual(chunks[0].chunk_id, persisted[0].chunk_id)

    def test_elasticsearch_index_serializes_bulk_and_search_requests(self) -> None:
        index = ElasticsearchAssetChunkIndex("http://es.test:9200")
        service = build_default_asset_ingestion_service()
        first_path = self.workspace / "es.txt"
        first_path.write_text("Hydration skincare premium serum", encoding="utf-8")
        chunks = service.ingest(
            AssetDescriptor(
                asset_id="asset-es-1",
                tenant_id="tenant-demo",
                project_id="project-demo",
                brand_id="brand-demo",
                name="ES 文档",
                kind="document",
                uri=str(first_path),
            )
        )

        with patch("src.asset_ingestion.httpx.Client") as client_cls:
            client = MagicMock()
            client.__enter__.return_value = client
            client.__exit__.return_value = None
            bulk_response = MagicMock()
            bulk_response.raise_for_status.return_value = None
            search_response = MagicMock()
            search_response.raise_for_status.return_value = None
            search_response.json.return_value = {
                "hits": {
                    "hits": [
                        {
                            "_source": {
                                "chunk_id": chunks[0].chunk_id,
                                "asset_id": chunks[0].asset_id,
                                "tenant_id": chunks[0].tenant_id,
                                "project_id": chunks[0].project_id,
                                "brand_id": chunks[0].brand_id,
                                "asset_kind": chunks[0].asset_kind,
                                "source_uri": chunks[0].source_uri,
                                "content_text": chunks[0].content_text,
                                "summary": chunks[0].summary,
                                "embedding": list(chunks[0].embedding),
                                "page_no": chunks[0].page_no,
                                "frame_no": chunks[0].frame_no,
                            }
                        }
                    ]
                }
            }
            client.post.side_effect = [bulk_response, search_response]
            client_cls.return_value = client

            index.upsert_chunks(chunks)
            results = index.search(
                DeterministicEmbeddingProvider().embed("premium hydration"),
                tenant_id="tenant-demo",
                brand_id="brand-demo",
            )

        self.assertEqual(client.post.call_count, 2)
        bulk_call = client.post.call_args_list[0]
        self.assertIn("/_bulk", bulk_call.args[0])
        search_call = client.post.call_args_list[1]
        self.assertIn("/asset_chunks/_search", search_call.args[0])
        self.assertEqual(results[0].asset_id, chunks[0].asset_id)


if __name__ == "__main__":
    unittest.main()
