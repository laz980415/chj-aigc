"""
模型供应商配置。
所有 API Key 和 endpoint 在此处配置，从环境变量读取，留空则该供应商不可用。

使用方式：
  在项目根目录创建 .env 文件，或直接设置系统环境变量。
  示例 .env：
    OPENAI_API_KEY=sk-xxx
    ALIYUN_API_KEY=xxx
    ...
"""

import os
from dataclasses import dataclass


@dataclass
class ProviderConfig:
    api_key: str
    api_base_url: str
    enabled: bool


def _cfg(key_env: str, base_url: str) -> ProviderConfig:
    api_key = os.getenv(key_env, "")
    return ProviderConfig(
        api_key=api_key,
        api_base_url=base_url,
        enabled=bool(api_key),
    )


# ── 供应商配置（填写对应环境变量即可启用）──────────────────────────────────

PROVIDERS: dict[str, ProviderConfig] = {

    # 全能助手 / 文案 / 深度研究 / 文生图 / 文生视频
    "openai": _cfg(
        "OPENAI_API_KEY",
        os.getenv("OPENAI_API_BASE", "https://api.openai.com/v1"),
    ),

    # 文生图 / 文生视频 / 图生视频 / 文生动漫 / 图生动漫 / 混合制作
    "aliyun": _cfg(
        "ALIYUN_API_KEY",
        os.getenv("ALIYUN_API_BASE", "https://dashscope.aliyuncs.com/api/v1"),
    ),

    # 全能助手 / 文生图
    "google": _cfg(
        "GOOGLE_API_KEY",
        os.getenv("GOOGLE_API_BASE", "https://generativelanguage.googleapis.com/v1beta"),
    ),

    # 文生视频 / 图生视频 (Seedance 2.0 / Helios)
    "bytedance": _cfg(
        "BYTEDANCE_API_KEY",
        os.getenv("BYTEDANCE_API_BASE", "https://ark.cn-beijing.volces.com/api/v3"),
    ),

    # 图生视频 / 图生动漫 (可灵 2.0 / O1)
    "kuaishou": _cfg(
        "KUAISHOU_API_KEY",
        os.getenv("KUAISHOU_API_BASE", "https://api.klingai.com"),
    ),

    # 文生动漫 (Niji 7)
    "midjourney": _cfg(
        "MIDJOURNEY_API_KEY",
        os.getenv("MIDJOURNEY_API_BASE", "https://api.midjourney.com/v1"),
    ),

    # AI 搜索
    "perplexity": _cfg(
        "PERPLEXITY_API_KEY",
        os.getenv("PERPLEXITY_API_BASE", "https://api.perplexity.ai"),
    ),

    # 全能助手 (Claude)
    "anthropic": _cfg(
        "ANTHROPIC_API_KEY",
        os.getenv("ANTHROPIC_API_BASE", "https://api.anthropic.com/v1"),
    ),
}


def get_provider(provider_id: str) -> ProviderConfig:
    cfg = PROVIDERS.get(provider_id)
    if not cfg:
        raise KeyError(f"未知供应商: {provider_id}")
    if not cfg.enabled:
        raise RuntimeError(f"供应商 {provider_id} 未配置 API Key，请检查环境变量")
    return cfg
