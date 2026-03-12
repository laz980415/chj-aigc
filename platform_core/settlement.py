from __future__ import annotations

from dataclasses import dataclass, field
from decimal import Decimal
from enum import Enum

from .billing import ChargeResult, Ledger, MeteredUsage, PricingBook, PriceUnit, VendorReference
from .generation import GenerationRequest, GenerationResult, JobStatus
from .models import CapabilityType, MeteringDimension, ModelRegistry


class QuotaDimension(str, Enum):
    TOKENS = "tokens"
    IMAGE_COUNT = "image_count"
    VIDEO_SECONDS = "video_seconds"
    DAILY_REQUESTS = "daily_requests"
    CONCURRENT_TASKS = "concurrent_tasks"


class QuotaScopeType(str, Enum):
    USER = "user"
    PROJECT = "project"


@dataclass(frozen=True)
class QuotaScope:
    scope_type: QuotaScopeType
    scope_id: str


@dataclass(frozen=True)
class QuotaAllocation:
    scope: QuotaScope
    dimension: QuotaDimension
    limit: Decimal
    used: Decimal = Decimal("0")

    @property
    def remaining(self) -> Decimal:
        return self.limit - self.used

    def consume(self, quantity: Decimal) -> "QuotaAllocation":
        if quantity < 0:
            raise ValueError("quantity must be non-negative")
        updated = self.used + quantity
        if updated > self.limit:
            raise ValueError("quota exceeded")
        return QuotaAllocation(
            scope=self.scope,
            dimension=self.dimension,
            limit=self.limit,
            used=updated,
        )


@dataclass
class QuotaBook:
    allocations: dict[tuple[QuotaScopeType, str, QuotaDimension], QuotaAllocation] = field(
        default_factory=dict
    )

    def upsert(self, allocation: QuotaAllocation) -> None:
        self.allocations[
            (allocation.scope.scope_type, allocation.scope.scope_id, allocation.dimension)
        ] = allocation

    def find(self, scope: QuotaScope, dimension: QuotaDimension) -> QuotaAllocation | None:
        return self.allocations.get((scope.scope_type, scope.scope_id, dimension))

    def ensure_available(self, scope: QuotaScope, dimension: QuotaDimension, quantity: Decimal) -> None:
        allocation = self.find(scope, dimension)
        if not allocation:
            raise ValueError(
                f"quota allocation not found for {scope.scope_type.value}:{scope.scope_id}:{dimension.value}"
            )
        if allocation.remaining < quantity:
            raise ValueError(
                f"quota exceeded for {scope.scope_type.value}:{scope.scope_id}:{dimension.value}"
            )

    def consume(self, scope: QuotaScope, dimension: QuotaDimension, quantity: Decimal) -> QuotaAllocation:
        allocation = self.find(scope, dimension)
        if not allocation:
            raise ValueError(
                f"quota allocation not found for {scope.scope_type.value}:{scope.scope_id}:{dimension.value}"
            )
        updated = allocation.consume(quantity)
        self.upsert(updated)
        return updated


@dataclass(frozen=True)
class UsageSettlement:
    job_id: str
    charge_result: ChargeResult
    price_unit: PriceUnit
    user_quota_consumption: tuple[tuple[QuotaDimension, Decimal], ...]
    project_quota_consumption: tuple[tuple[QuotaDimension, Decimal], ...]


def metered_usage_for_request(
    request: GenerationRequest,
    result: GenerationResult,
) -> tuple[MeteredUsage, ...]:
    capability = request.intent.capability
    if capability == CapabilityType.COPYWRITING:
        # Placeholder metering estimate until real provider token usage is wired in.
        input_tokens = Decimal(str(max(len(request.intent.user_prompt.split()), 1) * 12))
        output_tokens = Decimal(str(max(len(result.output_text.split()), 1) * 14))
        return (
            MeteredUsage(MeteringDimension.INPUT_TOKENS, input_tokens),
            MeteredUsage(MeteringDimension.OUTPUT_TOKENS, output_tokens),
        )
    if capability == CapabilityType.IMAGE_GENERATION:
        return (MeteredUsage(MeteringDimension.IMAGE_COUNT, Decimal("1")),)
    if capability == CapabilityType.VIDEO_GENERATION:
        seconds = Decimal("10")
        return (MeteredUsage(MeteringDimension.VIDEO_SECONDS, seconds),)
    raise ValueError(f"Unsupported capability: {capability}")


def quota_usage_for_metered_usage(metered_usage: tuple[MeteredUsage, ...]) -> tuple[tuple[QuotaDimension, Decimal], ...]:
    mapped: list[tuple[QuotaDimension, Decimal]] = []
    token_total = Decimal("0")
    for usage in metered_usage:
        if usage.dimension in (MeteringDimension.INPUT_TOKENS, MeteringDimension.OUTPUT_TOKENS):
            token_total += usage.quantity
        elif usage.dimension == MeteringDimension.IMAGE_COUNT:
            mapped.append((QuotaDimension.IMAGE_COUNT, usage.quantity))
        elif usage.dimension == MeteringDimension.VIDEO_SECONDS:
            mapped.append((QuotaDimension.VIDEO_SECONDS, usage.quantity))
    if token_total > 0:
        mapped.append((QuotaDimension.TOKENS, token_total))
    mapped.append((QuotaDimension.DAILY_REQUESTS, Decimal("1")))
    return tuple(mapped)


@dataclass
class SettlementCoordinator:
    registry: ModelRegistry
    pricing: PricingBook
    ledger: Ledger
    quota_book: QuotaBook

    def settle(
        self,
        *,
        request: GenerationRequest,
        result: GenerationResult,
        entry_id: str,
    ) -> UsageSettlement:
        if result.status not in {JobStatus.SUCCEEDED, JobStatus.PENDING}:
            raise ValueError("Only successful or pending provider results may be settled")

        route = self.registry.resolve_route(
            alias=request.model_alias,
            capability=request.intent.capability,
            require_brand_grounding=True,
            require_async_jobs=request.intent.capability == CapabilityType.VIDEO_GENERATION,
        )
        metered_usage = metered_usage_for_request(request, result)
        quota_usage = quota_usage_for_metered_usage(metered_usage)

        user_scope = QuotaScope(QuotaScopeType.USER, request.actor_id)
        project_scope = QuotaScope(QuotaScopeType.PROJECT, request.project_id)

        for dimension, quantity in quota_usage:
            self.quota_book.ensure_available(user_scope, dimension, quantity)
            self.quota_book.ensure_available(project_scope, dimension, quantity)

        for dimension, quantity in quota_usage:
            self.quota_book.consume(user_scope, dimension, quantity)
            self.quota_book.consume(project_scope, dimension, quantity)

        charge_result = self.pricing.quote(
            platform_model_id=route.platform_model.id,
            capability_key=route.platform_model.capability.value,
            usage=metered_usage,
        )
        self.ledger.bill_usage(
            entry_id=entry_id,
            tenant_id=request.tenant_id,
            description=f"Generation usage for job {request.job_id}",
            reference_id=request.job_id,
            reference_type="generation_task",
            model_id=route.platform_model.id,
            vendor_reference=VendorReference(
                provider_id=route.provider.id,
                provider_model_name=route.binding.provider_model_name,
            ),
            charge_result=charge_result,
        )

        return UsageSettlement(
            job_id=request.job_id,
            charge_result=charge_result,
            price_unit=charge_result.price_unit,
            user_quota_consumption=quota_usage,
            project_quota_consumption=quota_usage,
        )
