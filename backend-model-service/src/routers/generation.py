"""生成任务接口。

提供文案、图片、视频生成任务的提交、状态查询和结果获取。
"""

import sys
import os
import uuid
from typing import Optional
from fastapi import APIRouter, HTTPException
from pydantic import BaseModel

# 引用根目录 platform_core
sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "..", ".."))
from platform_core.generation import (
    GenerationRequest, GenerationResult, JobStatus,
    FakeProviderAdapter, GenerationOrchestrator,
)
from platform_core.grounding import BrandContext, GenerationIntent
from platform_core.grounding import AssetKind as GroundingAssetKind, GroundingAsset
from platform_core.models import CapabilityType, seed_default_registry
from platform_core.trace import resolve_trace_id, TraceLogger
from ..openai_adapter import OpenAIProviderAdapter

router = APIRouter()
logger = TraceLogger(__name__)

# 内存任务存储（生产环境替换为数据库）
_jobs: dict[str, GenerationResult] = {}
_adapter = OpenAIProviderAdapter(fallback=FakeProviderAdapter())
_registry = seed_default_registry()


class AssetInput(BaseModel):
    id: str
    name: str
    kind: str
    uri: str
    tags: list[str] = []
    summary: str = ""


class SubmitRequest(BaseModel):
    tenant_id: str
    project_id: str
    actor_id: str
    model_alias: str
    capability: str          # copywriting | image_generation | video_generation 等
    user_prompt: str
    brand_name: Optional[str] = None
    brand_summary: Optional[str] = None
    client_name: Optional[str] = None
    assets: list[AssetInput] = []
    trace_id: Optional[str] = None


class JobResponse(BaseModel):
    job_id: str
    status: str
    output_text: str = ""
    output_uri: str = ""
    error_message: str = ""
    provider_id: str = ""
    provider_model_name: str = ""
    provider_job_id: str = ""
    input_tokens: int = 0
    output_tokens: int = 0
    image_count: int = 0
    video_seconds: int = 0


@router.post("/jobs", response_model=JobResponse)
def submit_job(req: SubmitRequest):
    resolve_trace_id(req.trace_id)
    job_id = uuid.uuid4().hex
    logger.info("生成任务提交", job_id=job_id, model=req.model_alias, capability=req.capability)

    try:
        capability = CapabilityType(req.capability)
    except ValueError:
        raise HTTPException(status_code=400, detail=f"不支持的 capability: {req.capability}")

    brand_ctx = BrandContext(
        tenant_id=req.tenant_id,
        project_id=req.project_id,
        client_name=req.client_name or "默认客户",
        brand_name=req.brand_name or "默认品牌",
        brand_summary=req.brand_summary or "",
        assets=tuple(
            GroundingAsset(
                id=item.id,
                name=item.name,
                kind=GroundingAssetKind(item.kind.lower()),
                uri=item.uri,
                tags=tuple(item.tags),
                summary=item.summary,
            )
            for item in req.assets
        ),
    )
    intent = GenerationIntent(
        capability=capability,
        user_prompt=req.user_prompt,
    )
    gen_req = GenerationRequest(
        job_id=job_id,
        tenant_id=req.tenant_id,
        project_id=req.project_id,
        actor_id=req.actor_id,
        model_alias=req.model_alias,
        intent=intent,
        brand_context=brand_ctx,
    )

    orchestrator = GenerationOrchestrator(registry=_registry, adapter=_adapter)
    result = orchestrator.dispatch(gen_req)
    _jobs[job_id] = result

    logger.info("生成任务完成", job_id=job_id, status=result.status)
    return JobResponse(
        job_id=job_id,
        status=result.status,
        output_text=result.output_text,
        output_uri=result.output_uri,
        error_message=result.error_message,
        provider_id=result.provider_id,
        provider_model_name=result.provider_model_name,
        provider_job_id=result.provider_job_id,
        input_tokens=result.input_tokens,
        output_tokens=result.output_tokens,
        image_count=result.image_count,
        video_seconds=result.video_seconds,
    )


@router.get("/jobs/{job_id}", response_model=JobResponse)
def get_job(job_id: str):
    result = _jobs.get(job_id)
    if not result:
        raise HTTPException(status_code=404, detail="任务不存在")
    if result.status == JobStatus.PENDING and result.provider_job_id.startswith("provider-job-"):
        result = GenerationResult(
            job_id=result.job_id,
            status=JobStatus.SUCCEEDED,
            provider_id=result.provider_id,
            provider_model_name=result.provider_model_name,
            output_uri=f"oss://generated/{result.provider_model_name}/{job_id}.mp4",
            provider_job_id=result.provider_job_id,
            video_seconds=result.video_seconds or 10,
        )
        _jobs[job_id] = result
    return JobResponse(
        job_id=job_id,
        status=result.status,
        output_text=result.output_text,
        output_uri=result.output_uri,
        error_message=result.error_message,
        provider_id=result.provider_id,
        provider_model_name=result.provider_model_name,
        provider_job_id=result.provider_job_id,
        input_tokens=result.input_tokens,
        output_tokens=result.output_tokens,
        image_count=result.image_count,
        video_seconds=result.video_seconds,
    )
