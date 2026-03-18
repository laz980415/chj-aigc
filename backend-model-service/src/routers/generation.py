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
from platform_core.models import ModelRegistry, CapabilityType
from platform_core.trace import resolve_trace_id, TraceLogger

router = APIRouter()
logger = TraceLogger(__name__)

# 内存任务存储（生产环境替换为数据库）
_jobs: dict[str, GenerationResult] = {}
_adapter = FakeProviderAdapter()
_registry = ModelRegistry()


class SubmitRequest(BaseModel):
    tenant_id: str
    project_id: str
    actor_id: str
    model_alias: str
    capability: str          # copywriting | image | video
    raw_prompt: str
    brand_name: Optional[str] = None
    brand_summary: Optional[str] = None
    trace_id: Optional[str] = None


class JobResponse(BaseModel):
    job_id: str
    status: str
    output_text: str = ""
    output_uri: str = ""
    error_message: str = ""


@router.post("/jobs", response_model=JobResponse)
def submit_job(req: SubmitRequest):
    tid = resolve_trace_id(req.trace_id)
    job_id = uuid.uuid4().hex
    logger.info("生成任务提交", job_id=job_id, model=req.model_alias, capability=req.capability)

    try:
        capability = CapabilityType(req.capability)
    except ValueError:
        raise HTTPException(status_code=400, detail=f"不支持的 capability: {req.capability}")

    brand_ctx = BrandContext(
        brand_name=req.brand_name or "默认品牌",
        brand_summary=req.brand_summary or "",
        forbidden_statements=[],
        assets=[],
    )
    intent = GenerationIntent(
        capability=capability,
        raw_prompt=req.raw_prompt,
        target_format="text",
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
    result = orchestrator.run(gen_req)
    _jobs[job_id] = result

    logger.info("生成任务完成", job_id=job_id, status=result.status)
    return JobResponse(
        job_id=job_id,
        status=result.status,
        output_text=result.output_text,
        output_uri=result.output_uri,
        error_message=result.error_message,
    )


@router.get("/jobs/{job_id}", response_model=JobResponse)
def get_job(job_id: str):
    result = _jobs.get(job_id)
    if not result:
        raise HTTPException(status_code=404, detail="任务不存在")
    return JobResponse(
        job_id=job_id,
        status=result.status,
        output_text=result.output_text,
        output_uri=result.output_uri,
        error_message=result.error_message,
    )
