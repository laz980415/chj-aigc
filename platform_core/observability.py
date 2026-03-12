from __future__ import annotations

"""审计与安全观测模块。

负责记录生成摘要、费用摘要和安全违规信息，支撑平台后续排障和审计。
"""

from dataclasses import dataclass, field

from .billing import ChargeResult
from .generation import GenerationRequest, GenerationResult


@dataclass(frozen=True)
class SafetyPolicy:
    id: str
    name: str
    forbidden_terms: tuple[str, ...]
    active: bool = True


@dataclass(frozen=True)
class SafetyViolation:
    policy_id: str
    matched_term: str
    message: str


@dataclass(frozen=True)
class GenerationAuditRecord:
    job_id: str
    tenant_id: str
    project_id: str
    actor_id: str
    model_alias: str
    provider_id: str
    provider_model_name: str
    request_summary: str
    result_summary: str
    charge_total: str
    safety_violations: tuple[SafetyViolation, ...]


def summarize_request(request: GenerationRequest) -> str:
    prompt = request.intent.user_prompt.strip().replace("\n", " ")
    return prompt[:160]


def summarize_result(result: GenerationResult) -> str:
    if result.output_text:
        return result.output_text.strip().replace("\n", " ")[:160]
    if result.output_uri:
        return result.output_uri
    if result.provider_job_id:
        return f"pending:{result.provider_job_id}"
    return result.error_message[:160]


def evaluate_safety_policies(
    result: GenerationResult,
    policies: tuple[SafetyPolicy, ...],
) -> tuple[SafetyViolation, ...]:
    searchable = " ".join(
        part for part in (result.output_text, result.output_uri, result.error_message) if part
    ).lower()
    violations: list[SafetyViolation] = []
    for policy in policies:
        if not policy.active:
            continue
        for term in policy.forbidden_terms:
            if term.lower() in searchable:
                violations.append(
                    SafetyViolation(
                        policy_id=policy.id,
                        matched_term=term,
                        message=f"Matched forbidden term '{term}' in generation output",
                    )
                )
    return tuple(violations)


def build_generation_audit_record(
    request: GenerationRequest,
    result: GenerationResult,
    charge_result: ChargeResult,
    safety_policies: tuple[SafetyPolicy, ...] = (),
) -> GenerationAuditRecord:
    violations = evaluate_safety_policies(result, safety_policies)
    return GenerationAuditRecord(
        job_id=request.job_id,
        tenant_id=request.tenant_id,
        project_id=request.project_id,
        actor_id=request.actor_id,
        model_alias=request.model_alias,
        provider_id=result.provider_id,
        provider_model_name=result.provider_model_name,
        request_summary=summarize_request(request),
        result_summary=summarize_result(result),
        charge_total=str(charge_result.total),
        safety_violations=violations,
    )
