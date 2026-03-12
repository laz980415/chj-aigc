from __future__ import annotations

from dataclasses import dataclass, field
from enum import Enum

from .models import CapabilityType


class BrandRuleKind(str, Enum):
    TONE = "tone"
    STYLE_GUIDE = "style_guide"
    FORBIDDEN_STATEMENT = "forbidden_statement"
    REQUIRED_STATEMENT = "required_statement"
    SENSITIVE_TERM = "sensitive_term"


class AssetKind(str, Enum):
    IMAGE = "image"
    VIDEO = "video"
    DOCUMENT = "document"
    BRAND_GUIDE = "brand_guide"
    COPY_REFERENCE = "copy_reference"


@dataclass(frozen=True)
class GroundingAsset:
    id: str
    name: str
    kind: AssetKind
    uri: str
    tags: tuple[str, ...] = ()
    summary: str = ""


@dataclass(frozen=True)
class GroundingRule:
    id: str
    kind: BrandRuleKind
    content: str


@dataclass(frozen=True)
class BrandContext:
    tenant_id: str
    project_id: str
    client_name: str
    brand_name: str
    brand_summary: str
    rules: tuple[GroundingRule, ...] = ()
    assets: tuple[GroundingAsset, ...] = ()


@dataclass(frozen=True)
class GenerationIntent:
    capability: CapabilityType
    user_prompt: str
    audience: str = ""
    channel: str = ""
    objective: str = ""
    output_format: str = ""


@dataclass(frozen=True)
class PromptAuditRecord:
    tenant_id: str
    project_id: str
    brand_name: str
    capability: str
    selected_rule_ids: tuple[str, ...]
    selected_asset_ids: tuple[str, ...]


@dataclass(frozen=True)
class GroundedPrompt:
    system_prompt: str
    user_prompt: str
    context_summary: str
    audit: PromptAuditRecord


def assemble_grounded_prompt(
    brand_context: BrandContext,
    intent: GenerationIntent,
    max_assets: int = 5,
) -> GroundedPrompt:
    selected_assets = brand_context.assets[:max_assets]
    selected_rule_ids = tuple(rule.id for rule in brand_context.rules)
    selected_asset_ids = tuple(asset.id for asset in selected_assets)

    tone_rules = [rule.content for rule in brand_context.rules if rule.kind == BrandRuleKind.TONE]
    style_rules = [rule.content for rule in brand_context.rules if rule.kind == BrandRuleKind.STYLE_GUIDE]
    forbidden_rules = [
        rule.content for rule in brand_context.rules if rule.kind == BrandRuleKind.FORBIDDEN_STATEMENT
    ]
    required_rules = [
        rule.content for rule in brand_context.rules if rule.kind == BrandRuleKind.REQUIRED_STATEMENT
    ]
    sensitive_terms = [
        rule.content for rule in brand_context.rules if rule.kind == BrandRuleKind.SENSITIVE_TERM
    ]

    asset_lines = []
    for asset in selected_assets:
        tags = ", ".join(asset.tags)
        tag_suffix = f" | tags: {tags}" if tags else ""
        summary_suffix = f" | summary: {asset.summary}" if asset.summary else ""
        asset_lines.append(
            f"- [{asset.kind.value}] {asset.name}{tag_suffix}{summary_suffix} | uri: {asset.uri}"
        )

    system_sections = [
        "You are a brand-safe creative generation assistant.",
        f"Brand: {brand_context.brand_name}",
        f"Client: {brand_context.client_name}",
        f"Brand summary: {brand_context.brand_summary}",
        f"Capability: {intent.capability.value}",
    ]

    if tone_rules:
        system_sections.append("Tone guidance:\n- " + "\n- ".join(tone_rules))
    if style_rules:
        system_sections.append("Style guidance:\n- " + "\n- ".join(style_rules))
    if required_rules:
        system_sections.append("Required statements or angles:\n- " + "\n- ".join(required_rules))
    if forbidden_rules:
        system_sections.append("Forbidden claims or statements:\n- " + "\n- ".join(forbidden_rules))
    if sensitive_terms:
        system_sections.append("Sensitive terms to avoid or handle carefully:\n- " + "\n- ".join(sensitive_terms))
    if asset_lines:
        system_sections.append("Referenced brand assets:\n" + "\n".join(asset_lines))

    user_sections = [intent.user_prompt]
    if intent.objective:
        user_sections.append(f"Objective: {intent.objective}")
    if intent.audience:
        user_sections.append(f"Audience: {intent.audience}")
    if intent.channel:
        user_sections.append(f"Channel: {intent.channel}")
    if intent.output_format:
        user_sections.append(f"Output format: {intent.output_format}")

    context_summary = (
        f"Brand {brand_context.brand_name} with {len(brand_context.rules)} rules and "
        f"{len(selected_assets)} referenced assets."
    )

    return GroundedPrompt(
        system_prompt="\n\n".join(system_sections),
        user_prompt="\n".join(user_sections),
        context_summary=context_summary,
        audit=PromptAuditRecord(
            tenant_id=brand_context.tenant_id,
            project_id=brand_context.project_id,
            brand_name=brand_context.brand_name,
            capability=intent.capability.value,
            selected_rule_ids=selected_rule_ids,
            selected_asset_ids=selected_asset_ids,
        ),
    )
