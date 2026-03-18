from __future__ import annotations

"""Semantic asset retrieval for brand grounding."""

from dataclasses import dataclass

from .asset_ingestion import AssetContextRetriever


@dataclass(frozen=True)
class GroundingAssetRef:
    asset_id: str
    name: str
    kind: str
    uri: str
    tags: tuple[str, ...] = ()
    summary: str = ""


@dataclass(frozen=True)
class GroundingSnippet:
    asset_id: str
    asset_name: str
    asset_kind: str
    source_uri: str
    content_text: str
    summary: str
    page_no: int | None = None
    frame_no: int | None = None


@dataclass(frozen=True)
class GroundingContext:
    snippets: tuple[GroundingSnippet, ...]
    context_summary: str


class AssetGroundingService:
    def __init__(self, retriever: AssetContextRetriever) -> None:
        self.retriever = retriever

    def build_context(
        self,
        *,
        tenant_id: str,
        brand_id: str,
        user_prompt: str,
        assets: tuple[GroundingAssetRef, ...],
        limit: int = 5,
    ) -> GroundingContext:
        asset_lookup = {asset.asset_id: asset for asset in assets}
        retrieved = self.retriever.retrieve(
            user_prompt,
            tenant_id=tenant_id,
            brand_id=brand_id,
            asset_ids=tuple(asset_lookup.keys()),
            limit=limit,
        )
        snippets = tuple(
            GroundingSnippet(
                asset_id=chunk.asset_id,
                asset_name=asset_lookup.get(chunk.asset_id).name if chunk.asset_id in asset_lookup else chunk.asset_id,
                asset_kind=chunk.asset_kind,
                source_uri=chunk.source_uri,
                content_text=chunk.content_text,
                summary=chunk.summary,
                page_no=chunk.page_no,
                frame_no=chunk.frame_no,
            )
            for chunk in retrieved
        )
        return GroundingContext(
            snippets=snippets,
            context_summary=f"Retrieved {len(snippets)} semantic asset chunks for brand grounding.",
        )
