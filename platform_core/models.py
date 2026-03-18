from __future__ import annotations

"""模型注册与路由核心模块。

负责抽象供应商、平台模型别名、能力类型和路由优先级，便于后续替换模型厂商。
"""

from dataclasses import dataclass, field
from enum import Enum


class CapabilityType(str, Enum):
    CHAT = "chat"
    COPYWRITING = "copywriting"
    IMAGE_GENERATION = "image_generation"          # 文生图
    IMAGE_TO_VIDEO = "image_to_video"              # 图生视频
    VIDEO_GENERATION = "video_generation"          # 文生视频
    ANIME_GENERATION = "anime_generation"          # 文生动漫
    IMAGE_TO_ANIME = "image_to_anime"              # 图生动漫
    HYBRID_PRODUCTION = "hybrid_production"        # 混合制作
    AI_SEARCH = "ai_search"                        # AI搜索
    DEEP_RESEARCH = "deep_research"                # 深度研究
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

    # ── 供应商注册 ──────────────────────────────────────────────────────────
    registry.add_provider(
        Provider(id="openai", name="OpenAI", api_base_url="https://api.openai.com/v1"),
        credential=ProviderCredentialRef(provider_id="openai", secret_name="OPENAI_API_KEY"),
    )
    registry.add_provider(
        Provider(id="aliyun", name="阿里云百炼", api_base_url="https://dashscope.aliyuncs.com/api/v1"),
        credential=ProviderCredentialRef(provider_id="aliyun", secret_name="ALIYUN_API_KEY"),
    )
    registry.add_provider(
        Provider(id="google", name="Google", api_base_url="https://generativelanguage.googleapis.com/v1beta"),
        credential=ProviderCredentialRef(provider_id="google", secret_name="GOOGLE_API_KEY"),
    )
    registry.add_provider(
        Provider(id="bytedance", name="字节跳动", api_base_url="https://ark.cn-beijing.volces.com/api/v3"),
        credential=ProviderCredentialRef(provider_id="bytedance", secret_name="BYTEDANCE_API_KEY"),
    )
    registry.add_provider(
        Provider(id="kuaishou", name="快手可灵", api_base_url="https://api.klingai.com"),
        credential=ProviderCredentialRef(provider_id="kuaishou", secret_name="KUAISHOU_API_KEY"),
    )
    registry.add_provider(
        Provider(id="midjourney", name="Midjourney", api_base_url="https://api.midjourney.com/v1"),
        credential=ProviderCredentialRef(provider_id="midjourney", secret_name="MIDJOURNEY_API_KEY"),
    )
    registry.add_provider(
        Provider(id="perplexity", name="Perplexity", api_base_url="https://api.perplexity.ai"),
        credential=ProviderCredentialRef(provider_id="perplexity", secret_name="PERPLEXITY_API_KEY"),
    )
    registry.add_provider(
        Provider(id="anthropic", name="Anthropic", api_base_url="https://api.anthropic.com/v1"),
        credential=ProviderCredentialRef(provider_id="anthropic", secret_name="ANTHROPIC_API_KEY"),
    )

    # ── 平台模型定义 ────────────────────────────────────────────────────────

    # 全能助手
    for pm_id, alias, display, provider in [
        ("pm-chat-gpt",     "chat-gpt",     "ChatGPT",  "openai"),
        ("pm-chat-gemini",  "chat-gemini",  "Gemini",   "google"),
        ("pm-chat-claude",  "chat-claude",  "Claude",   "anthropic"),
    ]:
        registry.add_platform_model(PlatformModel(
            id=pm_id, alias=alias, display_name=display,
            capability=CapabilityType.CHAT,
            metering_dimensions=(MeteringDimension.INPUT_TOKENS, MeteringDimension.OUTPUT_TOKENS),
        ))

    # 文案生成
    registry.add_platform_model(PlatformModel(
        id="pm-copy-standard", alias="copy-standard", display_name="文案生成",
        capability=CapabilityType.COPYWRITING,
        metering_dimensions=(MeteringDimension.INPUT_TOKENS, MeteringDimension.OUTPUT_TOKENS),
        description="品牌安全文案生成，支持广告标题、描述、脚本等。",
    ))

    # 文生图
    registry.add_platform_model(PlatformModel(
        id="pm-image-qwen", alias="image-qwen", display_name="Qwen-Image-2.0 文生图",
        capability=CapabilityType.IMAGE_GENERATION,
        metering_dimensions=(MeteringDimension.IMAGE_COUNT,),
        description="阿里 Qwen-Image-2.0，支持品牌风格注入。",
    ))
    registry.add_platform_model(PlatformModel(
        id="pm-image-gemini", alias="image-gemini", display_name="Gemini 3.0 Pro Image 文生图",
        capability=CapabilityType.IMAGE_GENERATION,
        metering_dimensions=(MeteringDimension.IMAGE_COUNT,),
        description="谷歌 Gemini 3.0 Pro Image (Nano Banana Pro)。",
    ))
    registry.add_platform_model(PlatformModel(
        id="pm-image-dalle3", alias="image-dalle3", display_name="DALL·E 3 文生图",
        capability=CapabilityType.IMAGE_GENERATION,
        metering_dimensions=(MeteringDimension.IMAGE_COUNT,),
    ))
    registry.add_platform_model(PlatformModel(
        id="pm-image-standard", alias="image-standard", display_name="标准文生图",
        capability=CapabilityType.IMAGE_GENERATION,
        metering_dimensions=(MeteringDimension.IMAGE_COUNT,),
        description="兼容旧前端与测试用的标准文生图别名。",
    ))

    # 文生视频
    registry.add_platform_model(PlatformModel(
        id="pm-t2v-wan26", alias="t2v-wan26", display_name="Wan2.6-T2V 文生视频",
        capability=CapabilityType.VIDEO_GENERATION,
        metering_dimensions=(MeteringDimension.VIDEO_SECONDS,),
        description="阿里 Wan2.6 文生视频。",
    ))
    registry.add_platform_model(PlatformModel(
        id="pm-t2v-seedance", alias="t2v-seedance", display_name="Seedance 2.0 文生视频",
        capability=CapabilityType.VIDEO_GENERATION,
        metering_dimensions=(MeteringDimension.VIDEO_SECONDS,),
        description="字节跳动 Seedance 2.0。",
    ))
    registry.add_platform_model(PlatformModel(
        id="pm-t2v-sora2", alias="t2v-sora2", display_name="Sora 2 文生视频",
        capability=CapabilityType.VIDEO_GENERATION,
        metering_dimensions=(MeteringDimension.VIDEO_SECONDS,),
        description="OpenAI Sora 2，异步任务。",
    ))
    registry.add_platform_model(PlatformModel(
        id="pm-video-standard", alias="video-standard", display_name="标准文生视频",
        capability=CapabilityType.VIDEO_GENERATION,
        metering_dimensions=(MeteringDimension.VIDEO_SECONDS,),
        description="兼容旧前端与测试用的标准文生视频别名。",
    ))

    # 图生视频
    registry.add_platform_model(PlatformModel(
        id="pm-i2v-wan26", alias="i2v-wan26", display_name="Wan2.6-I2V 图生视频",
        capability=CapabilityType.IMAGE_TO_VIDEO,
        metering_dimensions=(MeteringDimension.VIDEO_SECONDS,),
        description="阿里 Wan2.6-I2V。",
    ))
    registry.add_platform_model(PlatformModel(
        id="pm-i2v-kling2", alias="i2v-kling2", display_name="可灵 2.0 图生视频",
        capability=CapabilityType.IMAGE_TO_VIDEO,
        metering_dimensions=(MeteringDimension.VIDEO_SECONDS,),
        description="快手可灵 2.0。",
    ))
    registry.add_platform_model(PlatformModel(
        id="pm-i2v-klingo1", alias="i2v-klingo1", display_name="可灵 O1 图生视频",
        capability=CapabilityType.IMAGE_TO_VIDEO,
        metering_dimensions=(MeteringDimension.VIDEO_SECONDS,),
        description="快手可灵 O1。",
    ))
    registry.add_platform_model(PlatformModel(
        id="pm-i2v-helios", alias="i2v-helios", display_name="Helios 图生视频",
        capability=CapabilityType.IMAGE_TO_VIDEO,
        metering_dimensions=(MeteringDimension.VIDEO_SECONDS,),
        description="北大&字节 Helios。",
    ))

    # 文生动漫
    registry.add_platform_model(PlatformModel(
        id="pm-anime-niji7", alias="anime-niji7", display_name="Midjourney Niji 7 文生动漫",
        capability=CapabilityType.ANIME_GENERATION,
        metering_dimensions=(MeteringDimension.IMAGE_COUNT,),
    ))
    registry.add_platform_model(PlatformModel(
        id="pm-anime-wan26", alias="anime-wan26", display_name="Wan2.6 文生动漫",
        capability=CapabilityType.ANIME_GENERATION,
        metering_dimensions=(MeteringDimension.IMAGE_COUNT,),
        description="阿里 Wan2.6 动漫风格。",
    ))

    # 图生动漫
    registry.add_platform_model(PlatformModel(
        id="pm-i2a-wan26", alias="i2a-wan26", display_name="Wan2.6-I2V 图生动漫",
        capability=CapabilityType.IMAGE_TO_ANIME,
        metering_dimensions=(MeteringDimension.IMAGE_COUNT,),
    ))
    registry.add_platform_model(PlatformModel(
        id="pm-i2a-klingo1", alias="i2a-klingo1", display_name="可灵 O1 图生动漫",
        capability=CapabilityType.IMAGE_TO_ANIME,
        metering_dimensions=(MeteringDimension.IMAGE_COUNT,),
    ))

    # 混合制作
    registry.add_platform_model(PlatformModel(
        id="pm-hybrid-aliyun", alias="hybrid-aliyun", display_name="Qwen-Image+Wan2.6 混合制作",
        capability=CapabilityType.HYBRID_PRODUCTION,
        metering_dimensions=(MeteringDimension.IMAGE_COUNT, MeteringDimension.VIDEO_SECONDS),
        description="阿里 Qwen-Image-2.0 + Wan2.6-I2V 组合流水线。",
    ))
    registry.add_platform_model(PlatformModel(
        id="pm-hybrid-openai", alias="hybrid-openai", display_name="DALL·E 3+Sora 2 混合制作",
        capability=CapabilityType.HYBRID_PRODUCTION,
        metering_dimensions=(MeteringDimension.IMAGE_COUNT, MeteringDimension.VIDEO_SECONDS),
        description="DALL·E 3 生图 + Sora 2 生视频组合流水线。",
    ))

    # AI 搜索
    registry.add_platform_model(PlatformModel(
        id="pm-search-perplexity", alias="search-perplexity", display_name="Perplexity AI搜索",
        capability=CapabilityType.AI_SEARCH,
        metering_dimensions=(MeteringDimension.INPUT_TOKENS, MeteringDimension.OUTPUT_TOKENS),
    ))

    # 深度研究
    registry.add_platform_model(PlatformModel(
        id="pm-research-chatgpt", alias="research-chatgpt", display_name="ChatGPT 深度研究",
        capability=CapabilityType.DEEP_RESEARCH,
        metering_dimensions=(MeteringDimension.INPUT_TOKENS, MeteringDimension.OUTPUT_TOKENS),
    ))

    # ── Provider 绑定 ───────────────────────────────────────────────────────

    # 全能助手绑定
    registry.bind_provider_model(ProviderModelBinding(
        id="bind-chat-gpt-4o", platform_model_id="pm-chat-gpt",
        provider_id="openai", provider_model_name="gpt-4o", priority=10,
    ))
    registry.bind_provider_model(ProviderModelBinding(
        id="bind-chat-gemini-2.0", platform_model_id="pm-chat-gemini",
        provider_id="google", provider_model_name="gemini-2.0-flash", priority=10,
    ))
    registry.bind_provider_model(ProviderModelBinding(
        id="bind-chat-claude-sonnet", platform_model_id="pm-chat-claude",
        provider_id="anthropic", provider_model_name="claude-sonnet-4-6", priority=10,
    ))

    # 文案绑定
    registry.bind_provider_model(ProviderModelBinding(
        id="bind-copy-gpt-4o", platform_model_id="pm-copy-standard",
        provider_id="openai", provider_model_name="gpt-4o", priority=10,
        supports_brand_grounding=True,
    ))

    # 文生图绑定
    registry.bind_provider_model(ProviderModelBinding(
        id="bind-image-qwen2", platform_model_id="pm-image-qwen",
        provider_id="aliyun", provider_model_name="qwen-vl-max", priority=10,
        supports_brand_grounding=True,
    ))
    registry.bind_provider_model(ProviderModelBinding(
        id="bind-image-gemini3", platform_model_id="pm-image-gemini",
        provider_id="google", provider_model_name="gemini-3.0-pro-image", priority=10,
        supports_brand_grounding=True,
    ))
    registry.bind_provider_model(ProviderModelBinding(
        id="bind-image-dalle3", platform_model_id="pm-image-dalle3",
        provider_id="openai", provider_model_name="dall-e-3", priority=10,
        supports_brand_grounding=True,
    ))
    registry.bind_provider_model(ProviderModelBinding(
        id="bind-image-standard-dalle3", platform_model_id="pm-image-standard",
        provider_id="openai", provider_model_name="dall-e-3", priority=10,
        supports_brand_grounding=True,
    ))

    # 文生视频绑定
    registry.bind_provider_model(ProviderModelBinding(
        id="bind-t2v-wan26", platform_model_id="pm-t2v-wan26",
        provider_id="aliyun", provider_model_name="wan2.6-t2v-turbo", priority=10,
        supports_async_jobs=True,
    ))
    registry.bind_provider_model(ProviderModelBinding(
        id="bind-t2v-seedance2", platform_model_id="pm-t2v-seedance",
        provider_id="bytedance", provider_model_name="seedance-2.0", priority=10,
        supports_async_jobs=True,
    ))
    registry.bind_provider_model(ProviderModelBinding(
        id="bind-t2v-sora2", platform_model_id="pm-t2v-sora2",
        provider_id="openai", provider_model_name="sora-2", priority=10,
        supports_async_jobs=True,
    ))
    registry.bind_provider_model(ProviderModelBinding(
        id="bind-video-standard-sora2", platform_model_id="pm-video-standard",
        provider_id="openai", provider_model_name="sora-2", priority=10,
        supports_async_jobs=True,
    ))

    # 图生视频绑定
    registry.bind_provider_model(ProviderModelBinding(
        id="bind-i2v-wan26", platform_model_id="pm-i2v-wan26",
        provider_id="aliyun", provider_model_name="wan2.6-i2v-turbo", priority=10,
        supports_async_jobs=True,
    ))
    registry.bind_provider_model(ProviderModelBinding(
        id="bind-i2v-kling2", platform_model_id="pm-i2v-kling2",
        provider_id="kuaishou", provider_model_name="kling-v2", priority=10,
        supports_async_jobs=True,
    ))
    registry.bind_provider_model(ProviderModelBinding(
        id="bind-i2v-klingo1", platform_model_id="pm-i2v-klingo1",
        provider_id="kuaishou", provider_model_name="kling-o1", priority=10,
        supports_async_jobs=True,
    ))
    registry.bind_provider_model(ProviderModelBinding(
        id="bind-i2v-helios", platform_model_id="pm-i2v-helios",
        provider_id="bytedance", provider_model_name="helios-i2v", priority=10,
        supports_async_jobs=True,
    ))

    # 文生动漫绑定
    registry.bind_provider_model(ProviderModelBinding(
        id="bind-anime-niji7", platform_model_id="pm-anime-niji7",
        provider_id="midjourney", provider_model_name="niji-7", priority=10,
        supports_async_jobs=True,
    ))
    registry.bind_provider_model(ProviderModelBinding(
        id="bind-anime-wan26", platform_model_id="pm-anime-wan26",
        provider_id="aliyun", provider_model_name="wan2.6-anime", priority=10,
        supports_async_jobs=True,
    ))

    # 图生动漫绑定
    registry.bind_provider_model(ProviderModelBinding(
        id="bind-i2a-wan26", platform_model_id="pm-i2a-wan26",
        provider_id="aliyun", provider_model_name="wan2.6-i2v-turbo", priority=10,
        supports_async_jobs=True,
    ))
    registry.bind_provider_model(ProviderModelBinding(
        id="bind-i2a-klingo1", platform_model_id="pm-i2a-klingo1",
        provider_id="kuaishou", provider_model_name="kling-o1", priority=10,
        supports_async_jobs=True,
    ))

    # 混合制作绑定（主供应商标记，实际由 pipeline 编排）
    registry.bind_provider_model(ProviderModelBinding(
        id="bind-hybrid-aliyun", platform_model_id="pm-hybrid-aliyun",
        provider_id="aliyun", provider_model_name="qwen-image-2.0+wan2.6-i2v", priority=10,
        supports_async_jobs=True,
    ))
    registry.bind_provider_model(ProviderModelBinding(
        id="bind-hybrid-openai", platform_model_id="pm-hybrid-openai",
        provider_id="openai", provider_model_name="dall-e-3+sora-2", priority=10,
        supports_async_jobs=True,
    ))

    # AI 搜索绑定
    registry.bind_provider_model(ProviderModelBinding(
        id="bind-search-perplexity", platform_model_id="pm-search-perplexity",
        provider_id="perplexity", provider_model_name="sonar-pro", priority=10,
    ))

    # 深度研究绑定
    registry.bind_provider_model(ProviderModelBinding(
        id="bind-research-o3", platform_model_id="pm-research-chatgpt",
        provider_id="openai", provider_model_name="o3", priority=10,
    ))

    return registry
