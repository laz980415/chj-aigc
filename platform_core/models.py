from __future__ import annotations

"""模型注册与路由核心模块。

负责抽象供应商、平台模型别名、能力类型和路由优先级，便于后续替换模型厂商。
"""

from dataclasses import dataclass, field
from enum import Enum


class CapabilityType(str, Enum):
    CHAT = "chat"
    COPYWRITING = "copywriting"
    IMAGE_GENERATION = "image_generation"
    VIDEO_GENERATION = "video_generation"
    EMBEDDING = "embedding"
    RERANK = "rerank"


class MeteringDimension(str, Enum):
    INPUT_TOKENS = "input_tokens"
    OUTPUT_TOKENS = "output_tokens"
    IMAGE_COUNT = "image_count"
    VIDEO_SECONDS = "video_seconds"


class ProviderStatus(str, Enum):
    ACTIVE = "active"
    DISABLED = "disabled"


class ModelStatus(str, Enum):
    ACTIVE = "active"
    DISABLED = "disabled"
    DEPRECATED = "deprecated"


@dataclass(frozen=True)
class Provider:
    id: str
    name: str
    api_base_url: str
    status: ProviderStatus = ProviderStatus.ACTIVE


@dataclass(frozen=True)
class ProviderCredentialRef:
    provider_id: str
    secret_name: str


@dataclass(frozen=True)
class PlatformModel:
    id: str
    alias: str
    display_name: str
    capability: CapabilityType
    metering_dimensions: tuple[MeteringDimension, ...]
    status: ModelStatus = ModelStatus.ACTIVE
    description: str = ""


@dataclass(frozen=True)
class ProviderModelBinding:
    id: str
    platform_model_id: str
    provider_id: str
    provider_model_name: str
    priority: int = 100
    status: ModelStatus = ModelStatus.ACTIVE
    supports_brand_grounding: bool = True
    supports_async_jobs: bool = False


@dataclass(frozen=True)
class ModelRoute:
    platform_model: PlatformModel
    provider: Provider
    binding: ProviderModelBinding


@dataclass
class ModelRegistry:
    providers: dict[str, Provider] = field(default_factory=dict)
    credentials: dict[str, ProviderCredentialRef] = field(default_factory=dict)
    platform_models: dict[str, PlatformModel] = field(default_factory=dict)
    bindings: list[ProviderModelBinding] = field(default_factory=list)

    def add_provider(self, provider: Provider, credential: ProviderCredentialRef | None = None) -> None:
        self.providers[provider.id] = provider
        if credential:
            if credential.provider_id != provider.id:
                raise ValueError("Credential provider_id does not match provider id")
            self.credentials[provider.id] = credential

    def add_platform_model(self, model: PlatformModel) -> None:
        self.platform_models[model.id] = model

    def bind_provider_model(self, binding: ProviderModelBinding) -> None:
        if binding.platform_model_id not in self.platform_models:
            raise ValueError(f"Unknown platform model: {binding.platform_model_id}")
        if binding.provider_id not in self.providers:
            raise ValueError(f"Unknown provider: {binding.provider_id}")
        self.bindings.append(binding)

    def resolve_route(
        self,
        alias: str,
        capability: CapabilityType | None = None,
        require_brand_grounding: bool = False,
        require_async_jobs: bool = False,
    ) -> ModelRoute:
        platform_model = self.find_platform_model(alias, capability)
        candidate_bindings = sorted(
            (
                binding
                for binding in self.bindings
                if binding.platform_model_id == platform_model.id
                and binding.status == ModelStatus.ACTIVE
                and self.providers[binding.provider_id].status == ProviderStatus.ACTIVE
                and (not require_brand_grounding or binding.supports_brand_grounding)
                and (not require_async_jobs or binding.supports_async_jobs)
            ),
            key=lambda item: item.priority,
        )
        if not candidate_bindings:
            raise LookupError(f"No active provider binding available for {platform_model.alias}")
        binding = candidate_bindings[0]
        return ModelRoute(
            platform_model=platform_model,
            provider=self.providers[binding.provider_id],
            binding=binding,
        )

    def find_platform_model(
        self,
        alias: str,
        capability: CapabilityType | None = None,
    ) -> PlatformModel:
        for model in self.platform_models.values():
            if model.alias != alias:
                continue
            if model.status != ModelStatus.ACTIVE:
                continue
            if capability and model.capability != capability:
                continue
            return model
        raise LookupError(f"Unknown active platform model alias: {alias}")

    def list_models_by_capability(self, capability: CapabilityType) -> list[PlatformModel]:
        return sorted(
            (
                model
                for model in self.platform_models.values()
                if model.capability == capability and model.status == ModelStatus.ACTIVE
            ),
            key=lambda item: item.alias,
        )


def seed_default_registry() -> ModelRegistry:
    registry = ModelRegistry()

    registry.add_provider(
        Provider(id="openai", name="OpenAI", api_base_url="https://api.openai.com/v1"),
        credential=ProviderCredentialRef(provider_id="openai", secret_name="OPENAI_API_KEY"),
    )
    registry.add_provider(
        Provider(id="stability", name="Stability", api_base_url="https://api.stability.ai"),
        credential=ProviderCredentialRef(provider_id="stability", secret_name="STABILITY_API_KEY"),
    )
    registry.add_provider(
        Provider(id="runway", name="Runway", api_base_url="https://api.runwayml.com"),
        credential=ProviderCredentialRef(provider_id="runway", secret_name="RUNWAY_API_KEY"),
    )

    registry.add_platform_model(
        PlatformModel(
            id="pm-copy-standard",
            alias="copy-standard",
            display_name="Copy Standard",
            capability=CapabilityType.COPYWRITING,
            metering_dimensions=(
                MeteringDimension.INPUT_TOKENS,
                MeteringDimension.OUTPUT_TOKENS,
            ),
            description="Primary copywriting model alias for tenant-facing use.",
        )
    )
    registry.add_platform_model(
        PlatformModel(
            id="pm-image-standard",
            alias="image-standard",
            display_name="Image Standard",
            capability=CapabilityType.IMAGE_GENERATION,
            metering_dimensions=(MeteringDimension.IMAGE_COUNT,),
            description="Primary text-to-image alias with vendor replaceability.",
        )
    )
    registry.add_platform_model(
        PlatformModel(
            id="pm-video-standard",
            alias="video-standard",
            display_name="Video Standard",
            capability=CapabilityType.VIDEO_GENERATION,
            metering_dimensions=(MeteringDimension.VIDEO_SECONDS,),
            description="Primary text-to-video alias using async vendor execution.",
        )
    )

    registry.bind_provider_model(
        ProviderModelBinding(
            id="bind-copy-openai-gpt-4.1",
            platform_model_id="pm-copy-standard",
            provider_id="openai",
            provider_model_name="gpt-4.1",
            priority=10,
            supports_brand_grounding=True,
        )
    )
    registry.bind_provider_model(
        ProviderModelBinding(
            id="bind-image-stability-sd3",
            platform_model_id="pm-image-standard",
            provider_id="stability",
            provider_model_name="stable-image-ultra",
            priority=10,
            supports_brand_grounding=True,
        )
    )
    registry.bind_provider_model(
        ProviderModelBinding(
            id="bind-video-runway-gen4",
            platform_model_id="pm-video-standard",
            provider_id="runway",
            provider_model_name="gen4_turbo",
            priority=10,
            supports_brand_grounding=True,
            supports_async_jobs=True,
        )
    )
    return registry
