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
from src.routers import generation as generation_router


class ModelServiceRouterTests(unittest.TestCase):
    def setUp(self) -> None:
        generation_router._jobs.clear()
        self.client = TestClient(app)

    def test_copy_job_returns_sync_result(self) -> None:
        response = self.client.post(
            "/api/model/jobs",
            json={
                "tenant_id": "tenant-demo",
                "project_id": "project-demo",
                "actor_id": "user-demo",
                "model_alias": "copy-standard",
                "capability": "copywriting",
                "user_prompt": "生成一条春季上新广告文案",
                "brand_name": "Acme Beauty",
                "brand_summary": "主打轻奢护肤。",
            },
        )

        self.assertEqual(response.status_code, 200)
        payload = response.json()
        self.assertEqual(payload["status"], "succeeded")
        self.assertEqual(payload["provider_id"], "openai")
        self.assertTrue(payload["output_text"])

    def test_video_job_can_be_polled_to_completion(self) -> None:
        create_response = self.client.post(
            "/api/model/jobs",
            json={
                "tenant_id": "tenant-demo",
                "project_id": "project-demo",
                "actor_id": "user-demo",
                "model_alias": "video-standard",
                "capability": "video_generation",
                "user_prompt": "生成一个 10 秒新品发布短视频",
                "brand_name": "Acme Beauty",
            },
        )

        self.assertEqual(create_response.status_code, 200)
        created = create_response.json()
        self.assertEqual(created["status"], "pending")
        self.assertTrue(created["provider_job_id"].startswith("provider-job-"))

        poll_response = self.client.get(f"/api/model/jobs/{created['job_id']}")

        self.assertEqual(poll_response.status_code, 200)
        polled = poll_response.json()
        self.assertEqual(polled["status"], "succeeded")
        self.assertTrue(polled["output_uri"].endswith(".mp4"))


if __name__ == "__main__":
    unittest.main()
