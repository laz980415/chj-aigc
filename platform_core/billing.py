from __future__ import annotations

from dataclasses import dataclass, field
from decimal import Decimal, ROUND_HALF_UP
from enum import Enum
from typing import Iterable

from .models import MeteringDimension


MONEY_PLACES = Decimal("0.0001")


class PriceUnit(str, Enum):
    CREDITS = "credits"
    CURRENCY = "currency"


class LedgerEntryType(str, Enum):
    RECHARGE = "recharge"
    USAGE = "usage"
    ADJUSTMENT = "adjustment"


@dataclass(frozen=True)
class MeteredUsage:
    dimension: MeteringDimension
    quantity: Decimal


@dataclass(frozen=True)
class PriceComponent:
    dimension: MeteringDimension
    unit_price: Decimal
    unit_size: Decimal = Decimal("1")

    def compute_cost(self, quantity: Decimal) -> Decimal:
        if self.unit_size <= 0:
            raise ValueError("unit_size must be positive")
        cost = (quantity / self.unit_size) * self.unit_price
        return cost.quantize(MONEY_PLACES, rounding=ROUND_HALF_UP)


@dataclass(frozen=True)
class PriceRule:
    id: str
    platform_model_id: str
    capability_key: str
    price_unit: PriceUnit
    components: tuple[PriceComponent, ...]
    active: bool = True
    tenant_tier: str | None = None
    description: str = ""


@dataclass(frozen=True)
class ChargeBreakdownLine:
    dimension: MeteringDimension
    quantity: Decimal
    unit_price: Decimal
    unit_size: Decimal
    subtotal: Decimal


@dataclass(frozen=True)
class VendorReference:
    provider_id: str
    provider_model_name: str
    raw_cost: Decimal | None = None
    raw_currency: str | None = None


@dataclass(frozen=True)
class ChargeResult:
    rule_id: str
    price_unit: PriceUnit
    total: Decimal
    breakdown: tuple[ChargeBreakdownLine, ...]


@dataclass(frozen=True)
class LedgerEntry:
    id: str
    tenant_id: str
    entry_type: LedgerEntryType
    amount: Decimal
    price_unit: PriceUnit
    description: str
    reference_type: str
    reference_id: str
    model_id: str | None = None
    vendor_reference: VendorReference | None = None
    charge_breakdown: tuple[ChargeBreakdownLine, ...] = ()


@dataclass
class PricingBook:
    rules: list[PriceRule] = field(default_factory=list)

    def add_rule(self, rule: PriceRule) -> None:
        self.rules.append(rule)

    def select_rule(
        self,
        platform_model_id: str,
        capability_key: str,
        tenant_tier: str | None = None,
    ) -> PriceRule:
        candidates = [
            rule
            for rule in self.rules
            if rule.active
            and rule.platform_model_id == platform_model_id
            and rule.capability_key == capability_key
            and (rule.tenant_tier == tenant_tier or rule.tenant_tier is None)
        ]
        if not candidates:
            raise LookupError(f"No active pricing rule for {platform_model_id}/{capability_key}")
        exact_tier = [rule for rule in candidates if rule.tenant_tier == tenant_tier]
        return exact_tier[0] if exact_tier else candidates[0]

    def quote(
        self,
        platform_model_id: str,
        capability_key: str,
        usage: Iterable[MeteredUsage],
        tenant_tier: str | None = None,
    ) -> ChargeResult:
        rule = self.select_rule(platform_model_id, capability_key, tenant_tier)
        usage_by_dimension = {item.dimension: item.quantity for item in usage}
        lines: list[ChargeBreakdownLine] = []
        total = Decimal("0")
        for component in rule.components:
            quantity = usage_by_dimension.get(component.dimension, Decimal("0"))
            subtotal = component.compute_cost(quantity)
            lines.append(
                ChargeBreakdownLine(
                    dimension=component.dimension,
                    quantity=quantity,
                    unit_price=component.unit_price,
                    unit_size=component.unit_size,
                    subtotal=subtotal,
                )
            )
            total += subtotal
        return ChargeResult(
            rule_id=rule.id,
            price_unit=rule.price_unit,
            total=total.quantize(MONEY_PLACES, rounding=ROUND_HALF_UP),
            breakdown=tuple(lines),
        )


@dataclass
class Ledger:
    entries: list[LedgerEntry] = field(default_factory=list)

    def add_entry(self, entry: LedgerEntry) -> None:
        self.entries.append(entry)

    def balance(self, tenant_id: str, price_unit: PriceUnit) -> Decimal:
        total = Decimal("0")
        for entry in self.entries:
            if entry.tenant_id == tenant_id and entry.price_unit == price_unit:
                total += entry.amount
        return total.quantize(MONEY_PLACES, rounding=ROUND_HALF_UP)

    def bill_usage(
        self,
        *,
        entry_id: str,
        tenant_id: str,
        description: str,
        reference_id: str,
        reference_type: str,
        model_id: str,
        vendor_reference: VendorReference,
        charge_result: ChargeResult,
    ) -> LedgerEntry:
        amount = -charge_result.total
        entry = LedgerEntry(
            id=entry_id,
            tenant_id=tenant_id,
            entry_type=LedgerEntryType.USAGE,
            amount=amount,
            price_unit=charge_result.price_unit,
            description=description,
            reference_type=reference_type,
            reference_id=reference_id,
            model_id=model_id,
            vendor_reference=vendor_reference,
            charge_breakdown=charge_result.breakdown,
        )
        self.add_entry(entry)
        return entry

    def record_recharge(
        self,
        *,
        entry_id: str,
        tenant_id: str,
        amount: Decimal,
        price_unit: PriceUnit,
        description: str,
        reference_id: str,
    ) -> LedgerEntry:
        entry = LedgerEntry(
            id=entry_id,
            tenant_id=tenant_id,
            entry_type=LedgerEntryType.RECHARGE,
            amount=amount.quantize(MONEY_PLACES, rounding=ROUND_HALF_UP),
            price_unit=price_unit,
            description=description,
            reference_type="recharge",
            reference_id=reference_id,
        )
        self.add_entry(entry)
        return entry


def seed_default_pricing() -> PricingBook:
    pricing = PricingBook()
    pricing.add_rule(
        PriceRule(
            id="price-copy-standard",
            platform_model_id="pm-copy-standard",
            capability_key="copywriting",
            price_unit=PriceUnit.CREDITS,
            components=(
                PriceComponent(
                    dimension=MeteringDimension.INPUT_TOKENS,
                    unit_price=Decimal("0.0020"),
                    unit_size=Decimal("1000"),
                ),
                PriceComponent(
                    dimension=MeteringDimension.OUTPUT_TOKENS,
                    unit_price=Decimal("0.0040"),
                    unit_size=Decimal("1000"),
                ),
            ),
            description="Default copywriting price in platform credits.",
        )
    )
    pricing.add_rule(
        PriceRule(
            id="price-image-standard",
            platform_model_id="pm-image-standard",
            capability_key="image_generation",
            price_unit=PriceUnit.CREDITS,
            components=(
                PriceComponent(
                    dimension=MeteringDimension.IMAGE_COUNT,
                    unit_price=Decimal("2.5000"),
                ),
            ),
            description="Default image generation price per image.",
        )
    )
    pricing.add_rule(
        PriceRule(
            id="price-video-standard",
            platform_model_id="pm-video-standard",
            capability_key="video_generation",
            price_unit=PriceUnit.CREDITS,
            components=(
                PriceComponent(
                    dimension=MeteringDimension.VIDEO_SECONDS,
                    unit_price=Decimal("4.0000"),
                ),
            ),
            description="Default video generation price per second.",
        )
    )
    return pricing
