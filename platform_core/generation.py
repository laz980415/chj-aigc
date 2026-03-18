from __future__ import annotations

"""生成任务编排模块。

负责把平台模型路由、品牌提示词和供应商调用适配成统一的生成任务流程。
"""

from dataclasses import dataclass, field
from enum import Enum

from .grounding import BrandContext, GenerationIntent, GroundedPrompt, assemble_grounded_prompt
from .models import CapabilityType, ModelRoute, ModelRegistry


class JobStatus(str, Enum):
    PENDING = "pending"
    RUNNING = "running"
    SUCCEEDED = "succeeded"
    FAILED = "failed"


@dataclass(frozen=True)
class GenerationRequest:
    job_id: str
    tenant_id: str
    project_id: str
    actor_id: str
    model_alias: str
    intent: GenerationIntent
    brand_context: BrandContext


@dataclass(frozen=True)
class ProviderInvocation:
    provider_id: str
    provider_model_name: str
    system_prompt: str
    user_prompt: str
    async_mode: bool


@dataclass(frozen=True)
class GenerationResult:
    job_id: str
    status: JobStatus
    provider_id: str
    provider_model_name: str
    output_text: str = ""
    output_uri: str = ""
    provider_job_id: str = ""
    error_message: str = ""
    input_tokens: int = 0
    output_tokens: int = 0
    image_count: int = 0
    video_seconds: int = 0


@dataclass(frozen=True)
class GenerationJob:
    request: GenerationRequest
    route: ModelRoute
    grounded_prompt: GroundedPrompt
    status: JobStatus


class ProviderAdapter:
    def invoke(self, route: ModelRoute, grounded_prompt: GroundedPrompt) -> GenerationResult:
        raise NotImplementedError


@dataclass
class FakeProviderAdapter(ProviderAdapter):
    """Test adapter that simulates sync copy/image and async video flows."""

    def invoke(self, route: ModelRoute, grounded_prompt: GroundedPrompt) -> GenerationResult:
        capability = route.platform_model.capability
        if capability == CapabilityType.COPYWRITING:
            input_tokens = max(len(grounded_prompt.user_prompt.split()), 1) * 12
            output_text = (
                f"[copy] Generated with brand {grounded_prompt.audit.brand_name}: "
                f"{grounded_prompt.user_prompt.splitlines()[0]}"
            )
            output_tokens = max(len(output_text.split()), 1) * 14
            return GenerationResult(
                job_id="",
                status=JobStatus.SUCCEEDED,
                provider_id=route.provider.id,
                provider_model_name=route.binding.provider_model_name,
                output_text=output_text,
                input_tokens=input_tokens,
                output_tokens=output_tokens,
            )
        if capability == CapabilityType.IMAGE_GENERATION:
            return GenerationResult(
                job_id="",
                status=JobStatus.SUCCEEDED,
                provider_id=route.provider.id,
                provider_model_name=route.binding.provider_model_name,
                output_uri=f"oss://generated/{route.platform_model.alias}/image.png",
                image_count=1,
            )
        if capability == CapabilityType.VIDEO_GENERATION:
            return GenerationResult(
                job_id="",
                status=JobStatus.PENDING,
                provider_id=route.provider.id,
                provider_model_name=route.binding.provider_model_name,
                provider_job_id=f"provider-job-{route.binding.id}",
                video_seconds=10,
            )
        raise ValueError(f"Unsupported capability: {capability}")


@dataclass
class GenerationOrchestrator:
    registry: ModelRegistry
    adapter: ProviderAdapter
    jobs: dict[str, GenerationJob] = field(default_factory=dict)

    def prepare_job(self, request: GenerationRequest) -> GenerationJob:
        require_async = request.intent.capability == CapabilityType.VIDEO_GENERATION
        route = self.registry.resolve_route(
            alias=request.model_alias,
            capability=request.intent.capability,
            require_brand_grounding=True,
            require_async_jobs=require_async,
        )
        grounded_prompt = assemble_grounded_prompt(request.brand_context, request.intent)
        job = GenerationJob(
            request=request,
            route=route,
            grounded_prompt=grounded_prompt,
            status=JobStatus.PENDING,
        )
        self.jobs[request.job_id] = job
        return job

    def dispatch(self, request: GenerationRequest) -> GenerationResult:
        job = self.prepare_job(request)
        result = self.adapter.invoke(job.route, job.grounded_prompt)
        final_result = GenerationResult(
            job_id=request.job_id,
            status=result.status,
            provider_id=result.provider_id,
            provider_model_name=result.provider_model_name,
            output_text=result.output_text,
            output_uri=result.output_uri,
            provider_job_id=result.provider_job_id,
            error_message=result.error_message,
            input_tokens=result.input_tokens,
            output_tokens=result.output_tokens,
            image_count=result.image_count,
            video_seconds=result.video_seconds,
        )
        self.jobs[request.job_id] = GenerationJob(
            request=job.request,
            route=job.route,
            grounded_prompt=job.grounded_prompt,
            status=final_result.status,
        )
        return final_result

    def mark_async_complete(self, job_id: str, output_uri: str) -> GenerationResult:
        job = self.jobs.get(job_id)
        if not job:
            raise KeyError(f"Unknown job: {job_id}")
        if job.request.intent.capability != CapabilityType.VIDEO_GENERATION:
            raise ValueError("Only video jobs require async completion")
        self.jobs[job_id] = GenerationJob(
            request=job.request,
            route=job.route,
            grounded_prompt=job.grounded_prompt,
            status=JobStatus.SUCCEEDED,
        )
        return GenerationResult(
            job_id=job_id,
            status=JobStatus.SUCCEEDED,
            provider_id=job.route.provider.id,
            provider_model_name=job.route.binding.provider_model_name,
            output_uri=output_uri,
            provider_job_id=f"provider-job-{job.route.binding.id}",
            video_seconds=10,
        )

    def build_provider_invocation(self, request: GenerationRequest) -> ProviderInvocation:
        job = self.prepare_job(request)
        return ProviderInvocation(
            provider_id=job.route.provider.id,
            provider_model_name=job.route.binding.provider_model_name,
            system_prompt=job.grounded_prompt.system_prompt,
            user_prompt=job.grounded_prompt.user_prompt,
            async_mode=request.intent.capability == CapabilityType.VIDEO_GENERATION,
        )
