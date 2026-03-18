"""OpenAI provider adapter with graceful fallback."""

from __future__ import annotations

import base64
from pathlib import Path
from typing import Any

import httpx

from platform_core.generation import FakeProviderAdapter, GenerationResult, JobStatus, ProviderAdapter
from platform_core.models import CapabilityType, ModelRoute
from .provider_config import get_provider


class OpenAIProviderAdapter(ProviderAdapter):
    def __init__(self, fallback: ProviderAdapter | None = None) -> None:
        self.fallback = fallback or FakeProviderAdapter()
        self.output_dir = Path(__file__).resolve().parent.parent / "generated"
        self.output_dir.mkdir(parents=True, exist_ok=True)

    def invoke(self, route: ModelRoute, grounded_prompt) -> GenerationResult:
        if route.provider.id != "openai":
            return self.fallback.invoke(route, grounded_prompt)
        try:
            config = get_provider("openai")
        except RuntimeError:
            return self.fallback.invoke(route, grounded_prompt)

        capability = route.platform_model.capability
        try:
            if capability == CapabilityType.COPYWRITING:
                return self._invoke_chat(route, grounded_prompt, config.api_base_url, config.api_key)
            if capability == CapabilityType.IMAGE_GENERATION:
                return self._invoke_image(route, grounded_prompt, config.api_base_url, config.api_key)
        except httpx.HTTPError:
            raise
        except Exception:
            raise
        return self.fallback.invoke(route, grounded_prompt)

    def _invoke_chat(
        self,
        route: ModelRoute,
        grounded_prompt,
        base_url: str,
        api_key: str,
    ) -> GenerationResult:
        payload = {
            "model": route.binding.provider_model_name,
            "messages": [
                {"role": "system", "content": grounded_prompt.system_prompt},
                {"role": "user", "content": grounded_prompt.user_prompt},
            ],
        }
        data = self._post_json(f"{base_url}/chat/completions", api_key, payload)
        choice = (data.get("choices") or [{}])[0]
        message = choice.get("message") or {}
        content = self._extract_text(message.get("content"))
        usage = data.get("usage") or {}
        return GenerationResult(
            job_id="",
            status=JobStatus.SUCCEEDED,
            provider_id=route.provider.id,
            provider_model_name=route.binding.provider_model_name,
            output_text=content.strip(),
            input_tokens=int(usage.get("prompt_tokens") or 0),
            output_tokens=int(usage.get("completion_tokens") or 0),
        )

    def _invoke_image(
        self,
        route: ModelRoute,
        grounded_prompt,
        base_url: str,
        api_key: str,
    ) -> GenerationResult:
        prompt = f"{grounded_prompt.system_prompt}\n\n{grounded_prompt.user_prompt}".strip()
        payload = {
            "model": route.binding.provider_model_name,
            "prompt": prompt,
            "size": "1024x1024",
            "response_format": "b64_json",
        }
        data = self._post_json(f"{base_url}/images/generations", api_key, payload)
        item = (data.get("data") or [{}])[0]
        output_uri = self._save_image(item.get("b64_json")) if item.get("b64_json") else str(item.get("url") or "")
        return GenerationResult(
            job_id="",
            status=JobStatus.SUCCEEDED,
            provider_id=route.provider.id,
            provider_model_name=route.binding.provider_model_name,
            output_uri=output_uri,
            image_count=1,
        )

    def _post_json(self, url: str, api_key: str, payload: dict[str, Any]) -> dict[str, Any]:
        headers = {
            "Authorization": f"Bearer {api_key}",
            "Content-Type": "application/json",
        }
        with httpx.Client(timeout=120) as client:
            response = client.post(url, headers=headers, json=payload)
            response.raise_for_status()
            return response.json()

    def _save_image(self, b64_json: str) -> str:
        data = base64.b64decode(b64_json)
        target = self.output_dir / f"image-{len(list(self.output_dir.glob('image-*.png'))) + 1}.png"
        target.write_bytes(data)
        return str(target)

    def _extract_text(self, content: Any) -> str:
        if isinstance(content, str):
            return content
        if isinstance(content, list):
            parts: list[str] = []
            for item in content:
                if isinstance(item, dict):
                    if item.get("type") == "text" and item.get("text"):
                        parts.append(str(item["text"]))
                    elif item.get("type") == "output_text" and item.get("text"):
                        parts.append(str(item["text"]))
            if parts:
                return "\n".join(parts)
        return ""
